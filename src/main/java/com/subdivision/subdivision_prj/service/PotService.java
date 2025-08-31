package com.subdivision.subdivision_prj.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.subdivision.subdivision_prj.domain.*;
import com.subdivision.subdivision_prj.domain.specification.PotSpecification;
import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 팟(Pot) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 생성, 조회, 수정, 삭제, 참여, 검색 등 팟에 대한 핵심 기능을 담당합니다.
 * @author subdivision
 */
@Service
public class PotService {

    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final PotMemberRepository potMemberRepository;
    private final AmazonS3 amazonS3Client; // local 환경에서는 주입되지 않고 null이 될 수 있습니다.
    private final String bucket;

    /**
     * PotService의 생성자입니다. 의존성 주입(Dependency Injection)을 담당합니다.
     * @param amazonS3Client @Autowired(required = false)를 통해 S3 기능이 비활성화된 환경(예: local)에서는 null이 주입되어 오류를 방지합니다.
     * @param bucket @Value 어노테이션을 통해 application.properties에 설정된 S3 버킷 이름을 주입받습니다. 값이 없을 경우 빈 문자열이 주입됩니다.
     */
    public PotService(
            PotRepository potRepository,
            UserRepository userRepository,
            PotMemberRepository potMemberRepository,
            @Autowired(required = false) AmazonS3 amazonS3Client,
            @Value("${cloud.aws.s3.bucket:}") String bucket
    ) {
        this.potRepository = potRepository;
        this.userRepository = userRepository;
        this.potMemberRepository = potMemberRepository;
        this.amazonS3Client = amazonS3Client;
        this.bucket = bucket;
    }

    /**
     * 인증되지 않은 사용자도 볼 수 있는 전체 팟 목록을 페이징하여 조회합니다.
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기 등)
     * @return 페이징 처리된 팟 정보 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<PotResponseDto> getAllPotsPublic(Pageable pageable) {
        Page<Pot> pots = potRepository.findAll(pageable);
        // 각 Pot 엔티티를 PotResponseDto로 변환하되, 이미지 URL은 Presigned URL로 생성하여 포함시킵니다.
        return pots.map(this::createPotResponseDtoWithPresignedUrl);
    }

    /**
     * 새로운 팟(Pot)을 생성합니다.
     * @param requestDto 팟 생성에 필요한 정보를 담은 DTO
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 생성된 팟의 상세 정보를 담은 DTO
     */
    @Transactional
    public PotResponseDto createPot(PotCreateRequestDto requestDto, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Pot newPot = requestDto.toEntity(currentUser);
        Pot savedPot = potRepository.save(newPot);

        // 생성된 팟 정보를 DTO로 변환하여 반환합니다. 이 과정에서 이미지 URL이 Presigned URL로 변환됩니다.
        return createPotResponseDtoWithPresignedUrl(savedPot);
    }

    /**
     * 특정 ID를 가진 팟의 상세 정보를 조회합니다.
     * @param potId 조회할 팟의 ID
     * @param userDetails 현재 인증된 사용자의 정보 (참여 여부 확인용)
     * @return 조회된 팟의 상세 정보를 담은 DTO
     */
    @Transactional(readOnly = true)
    public PotResponseDto getPotById(Long potId, UserDetails userDetails) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다. ID=" + potId));

        PotResponseDto responseDto = createPotResponseDtoWithPresignedUrl(pot);

        // 로그인한 사용자의 경우, 해당 팟에 참여했는지 여부를 확인하여 DTO에 설정합니다.
        if(userDetails != null) {
            User currentUser = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            boolean isJoined = pot.getMembers().stream()
                    .anyMatch(member -> member.getUser().equals(currentUser));
            responseDto.setCurrentUserJoined(isJoined);
        }
        return responseDto;
    }

    /**
     * 페이징 없이 모든 팟 목록을 조회합니다.
     * @return 모든 팟의 정보를 담은 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PotResponseDto> getAllPots() {
        return potRepository.findAll().stream()
                .map(this::createPotResponseDtoWithPresignedUrl)
                .collect(Collectors.toList());
    }

    /**
     * 기존 팟의 정보를 수정합니다.
     * 이미지를 변경하지 않았을 때 발생하는 URL 중첩 문제를 해결하는 핵심 로직이 포함되어 있습니다.
     * @param potId 수정할 팟의 ID
     * @param requestDto 수정할 내용을 담은 DTO
     * @param userDetails 현재 인증된 사용자 정보
     * @return 수정된 팟의 상세 정보를 담은 DTO
     */
    @Transactional
    public PotResponseDto updatePot(Long potId, PotUpdateRequestDto requestDto, UserDetails userDetails) {
        // 1. 수정하려는 팟을 DB에서 찾고, 현재 로그인한 사용자가 작성자인지 확인합니다.
        Pot pot = findPotAndCheckOwnership(potId, userDetails.getUsername());

        // 2. [핵심 로직] 프론트엔드에서 받은 imageUrl이 무엇인지 판별합니다.
        String newImageUrl = requestDto.getImageUrl();

        // 2-1. 이미지를 변경하지 않았다면: 프론트엔드는 기존에 받았던 완전한 Presigned URL (https://...)을 그대로 다시 보냅니다.
        // 2-2. 이미지를 새로 업로드했다면: 프론트엔드는 S3Uploader로부터 받은 새로운 파일 경로 (images/...)를 보냅니다.
        // 따라서, URL이 'http'로 시작하지 않는다면, 그것은 새로운 이미지 경로임을 의미합니다.
        boolean isNewImageUploaded = newImageUrl != null && !newImageUrl.startsWith("http");

        // 3. Pot 엔티티의 update 메서드를 호출할 때, 이미지 변경 여부를 함께 전달합니다.
        pot.update(requestDto, isNewImageUploaded);

        // 4. 수정된 정보를 다시 DTO로 변환하여 반환합니다.
        return createPotResponseDtoWithPresignedUrl(pot);
    }

    /**
     * 팟을 삭제합니다.
     * @param potId 삭제할 팟의 ID
     * @param userDetails 현재 인증된 사용자 정보
     */
    @Transactional
    public void deletePot(Long potId, UserDetails userDetails) {
        Pot pot = findPotAndCheckOwnership(potId, userDetails.getUsername());
        potRepository.delete(pot);
    }

    /**
     * 수정/삭제 권한 확인을 위해 팟을 조회하고 작성자 본인 여부를 검증하는 private 헬퍼 메서드입니다.
     */
    private Pot findPotAndCheckOwnership(Long potId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다. ID=" + potId));
        if(!pot.getUser().equals(currentUser)) {
            throw new IllegalArgumentException("이 팟을 수정하거나 삭제할 권한이 없습니다.");
        }
        return pot;
    }

    /**
     * 현재 사용자가 특정 팟에 참여합니다.
     */
    @Transactional
    public void joinPot(Long potId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다."));

        // 이미 참여했는지 확인하여 중복 참여를 방지합니다.
        potMemberRepository.findByPotAndUser(pot, currentUser).ifPresent(m -> {
            throw new IllegalArgumentException("이미 참여한 팟입니다.");
        });

        // Pot 엔티티 내부 로직을 통해 참여자 수를 늘리고, 상태를 변경합니다.
        pot.addParticipant();

        // PotMember 테이블에 참여 관계를 기록합니다.
        PotMember potMember = PotMember.builder()
                .pot(pot)
                .user(currentUser)
                .build();
        potMemberRepository.save(potMember);
    }

    /**
     * 현재 사용자가 참여했던 팟에서 나갑니다.
     */
    @Transactional
    public void leavePot(Long potId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다."));

        PotMember potMember = potMemberRepository.findByPotAndUser(pot, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("이 팟의 멤버가 아닙니다."));

        pot.removeParticipant();
        potMemberRepository.delete(potMember);
    }

    /**
     * 다양한 조건(키워드, 카테고리, 상태, 거리)을 조합하여 팟을 동적으로 검색하고 페이징하여 반환합니다.
     */
    @Transactional(readOnly = true)
    public Page<PotResponseDto> searchPots(Double lat, Double lon, Double distance, String keyword,
                                           PotCategory category, PotStatus status, Pageable pageable) {
        // Specification을 사용하여 키워드, 카테고리, 상태에 대한 동적 쿼리를 생성합니다.
        Specification<Pot> spec = Specification.where(null);
        if(keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(PotSpecification.likeKeyword(keyword));
        }
        if(category != null) {
            spec = spec.and(PotSpecification.equalCategory(category));
        }
        if(status != null) {
            spec = spec.and(PotSpecification.equalStatus(status));
        }

        // 1차적으로 DB에서 조건에 맞는 데이터를 모두 조회합니다.
        List<Pot> filteredPots = potRepository.findAll(spec);

        // 2차적으로 메모리에서 Haversine 공식을 사용하여 거리 기반 필터링을 수행합니다.
        List<Pot> nearbyPots = filteredPots.stream()
                .filter(pot -> isWithinDistance(lat, lon, pot.getLatitude(), pot.getLongitude(), distance))
                .toList();

        // 거리까지 필터링된 최종 목록을 수동으로 페이징 처리합니다.
        int start = (int)pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), nearbyPots.size());
        List<Pot> pageContent = nearbyPots.subList(start, end);

        // 최종 결과를 Page 객체로 만들어 반환합니다.
        return new PageImpl<>(
                pageContent.stream().map(this::createPotResponseDtoWithPresignedUrl).collect(Collectors.toList()),
                pageable,
                nearbyPots.size()
        );
    }

    /**
     * 두 지점 간의 거리를 계산하는 Haversine 공식 헬퍼 메서드입니다.
     */
    private boolean isWithinDistance(double lat1, double lon1, double lat2, double lon2, double distanceKm) {
        final int R = 6371; // 지구의 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (R * c) <= distanceKm;
    }

    /**
     * Pot 엔티티를 PotResponseDto로 변환하는 헬퍼 메서드입니다.
     * 이 과정에서 DB에 저장된 이미지 경로(Key)를 임시 접근 가능한 Presigned URL로 변환합니다.
     */
    public PotResponseDto createPotResponseDtoWithPresignedUrl(Pot pot) {
        PotResponseDto responseDto = new PotResponseDto(pot);
        String imageKey = pot.getImageUrl();

        // S3 기능이 활성화된 환경에서만 Presigned URL을 생성합니다.
        if (imageKey != null && !imageKey.isEmpty() && this.amazonS3Client != null) {
            responseDto.setImageUrl(generatePresignedUrl(imageKey));
        }
        return responseDto;
    }

    /**
     * S3 파일 키를 받아, 제한된 시간 동안만 유효한 임시 접근 URL(Presigned URL)을 생성합니다.
     */
    private String generatePresignedUrl(String fileKey) {
        if (this.amazonS3Client == null) return fileKey; // S3 비활성화 시 원본 키 반환

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime() + (60 * 60 * 1000); // 유효기간: 1시간
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, fileKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL url = amazonS3Client.generatePresignedUrl(request);
        return url.toString();
    }
}

