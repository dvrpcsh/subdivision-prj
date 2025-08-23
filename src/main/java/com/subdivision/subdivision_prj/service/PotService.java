package com.subdivision.subdivision_prj.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.subdivision.subdivision_prj.domain.*;
import com.subdivision.subdivision_prj.domain.specification.PotSpecification;
import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줍니다
public class PotService {

    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final PotMemberRepository potMemberRepository;
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 새로운 팟(Pot)을 생성하는 메서드
     * @param requestDto 팟 생성 요청 데이터
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 생성된 팟의 정보를 담은 DTO
     */
    @Transactional //이 메서드 내의 모든 데이터베이스 작업은 하나의 트랜잭션으로 처리됩니다.
    public PotResponseDto createPot(PotCreateRequestDto requestDto, UserDetails userDetails) {
        //1.현재 로그인 한 사용자의 이메일을 기반으로 User 엔티티를 찾습니다.
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        //2.DTO와 현재 사용자 정보를 바탕으로 Pot 엔티티를 생성합니다.
        // (DTO의 toEntity 메서드를 활용)
        Pot newPot = requestDto.toEntity(currentUser);

        //3.생성된 Pot 엔티티를 데이터베이스에 저장합니다.
        Pot savedPot = potRepository.save(newPot);

        //4.DTO를 생성하고, 이미지 URL이 있다면 사전 서명된 URL을 생성하여 설정합니다.
        return createPotResponseDtoWithPresignedUrl(savedPot);
    }

    /**
     * 특정 ID의 팟을 조회하는 메서드
     * @param potId 조회할 팟의 ID
     * @return 조회된 팟의 정보를 담은 DTO
     */
    @Transactional(readOnly = true) //데이터를 변경하지 않은 조회 작업이므로 readOnly = true
    public PotResponseDto getPotById(Long potId) {
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다. ID=" + potId));

        //DTO를 생성하고, 이미지 URL이 있다면 사전 서명된 URL을 생성하여 설정합니다.
        return createPotResponseDtoWithPresignedUrl(pot);
    }

    /**
     * 모든 팟의 목록을 조회하는 메서드
     * @return 모든 팟의 정보를 담은 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PotResponseDto> getAllPots() {

        //각 Pot에 대해 사전 서명된 URL을 생성하여 DTO 리스트를 만듭니다.
        return potRepository.findAll().stream()
                .map(this::createPotResponseDtoWithPresignedUrl)
                .collect(Collectors.toList());
    }

    /**
     * 팟(Pot)을 수정하는 메서드
     */
    @Transactional
    public PotResponseDto updatePot(Long potId, PotUpdateRequestDto requestDto, UserDetails userDetails) {
        //1.게시물 존재 여부 및 작성자 본인 여부 확인
        Pot pot = findPotAndCheckOwnership(potId, userDetails.getUsername());

        //2.Pot 엔티티에 정의한 update 메서드를 사용하여 데이터 변경
        pot.update(requestDto);

        //DTO를 생성하고, 이미지 URL이 있다면 사전 서명된 URL을 생성하여 설정합니다.
        return createPotResponseDtoWithPresignedUrl(pot);
    }

    /**
     * 팟(Pot)을 삭제하는 메서드
     */
    @Transactional
    public void deletePot(Long potId, UserDetails userDetails) {
        //1.게시물 존재 여부 및 작성자 본인 여부 확인
        Pot pot = findPotAndCheckOwnership(potId, userDetails.getUsername());

        //2.확인된 팟 삭제
        potRepository.delete(pot);
    }

    /**
     * 게시물 ID로 팟을 찾고, 현재 사용자가 작성자인지 확인하는 private메서드
     * @return 확인된 Pot 엔티티
     */
    private Pot findPotAndCheckOwnership(Long potId, String userEmail) {
        //현재 사용자 정보 조회
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        //조회하려는 팟 정보
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다. ID=" + potId));

        //팟의 작성자와 현재 사용자가 동일한지 확인
        if(!pot.getUser().equals(currentUser)) {
            //동일하지 않다면 권한 없음 예외 발생
            throw new IllegalArgumentException("이 팟을 수정하거나 삭제할 권한이 없습니다.");
        }

        return pot;
    }

    /**
     * 팟에 참여하는 메서드
     */
    @Transactional
    public void joinPot(Long potId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다."));

        //이미 참여한 사용자인지 확인
        potMemberRepository.findByPotAndUser(pot, currentUser).ifPresent(m -> {
            throw new IllegalArgumentException("이미 참여한 팟입니다.");
        });

        //팟 인원 수 1증가(내부적으로 마감 여부 체크)
        pot.addParticipant();

        //PotMember에 참여 정보 저장
        PotMember potMember = PotMember.builder()
                .pot(pot)
                .user(currentUser)
                .build();
        potMemberRepository.save(potMember);
    }

    /**
     * 팟 참여를 취소하는 메서드
     */
    @Transactional
    public void leavePot(Long potId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 팟을 찾을 수 없습니다."));

        //참여 정보를 찾음. 없으면 예외 발생
        PotMember potMember = potMemberRepository.findByPotAndUser(pot, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("이 팟의 멤버가 아닙니다."));

        //팟 인원 수 1감소(내부적으로 작성자인지 체크)
        pot.removeParticipant();

        //PotMember에서 참여 정보 삭제
        potMemberRepository.delete(potMember);
    }

    /**
     * 2025-08-18 거리에 따른 동적 팟 검색 메서드로 대체
     * 위치 기반 검색 메서드
     *
     * 지정된 좌표를 중심으로 특정 반경 내의 모든 팟(Pot)을 검색합니다.
     * 데이터베이스(PostGIS)의 공간 쿼리 성능을 활용하는 핵심 비즈니스 로직입니다.
     *
     * @param lon 검색 중심점의 경도(longitude)
     * @param lat 검색 중심점의 위도(latitude)
     * @param distance 검색 반경(단위:km)
     * @return 검색 조건에 맞는 팟의 목록을 담은 DTO 리스트

    @Transactional(readOnly = true)
    public List<PotResponseDto> findPotsByLocation(Double lon, Double lat, Double distance) {
        //PotRepository에 정의한 Native Query를 호출하여 엔티티 리스트를 조회합니다.
        List<Pot> pots = potRepository.findPotsByLocation(lon, lat, distance);

        //각 Pot에 대해 사전 서명된 URL을 생성하여 DTO 리스트를 만듭니다.
        return pots.stream()
                .map(this::createPotResponseDtoWithPresignedUrl)
                .collect(Collectors.toList());
    }
    */

    /**
     * 복합 조건에 따라 동적으로 팟을 검색하는 메서드
     */
    @Transactional(readOnly = true)
    public Page<PotResponseDto> searchPots(Double lat, Double lon, Double distance, String keyword,
                                           PotCategory category, PotStatus status, Pageable pageable) {
        //1.기본 Specification을 생성합니다.
        Specification<Pot> spec = (root, query, criteriaBuilder) -> null;

        //2.각 파라미터가 존재할 경우, 해당 조건의 Specification을 AND로 추가합니다.
        if(keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(PotSpecification.likeKeyword(keyword));
        }

        if(category != null) {
            spec = spec.and(PotSpecification.equalCategory(category));
        }

        if(status != null) {
            spec = spec.and(PotSpecification.equalStatus(status));
        }

        //3.Specification으로 1차 필터링 된 결과 조회(페이징 적용)
        Page<Pot> filteredPots = potRepository.findAll(spec, pageable);

        //4.위치 기반 필터링은 Specification으로 처리하기 복잡하므로, 1차 결과 내에서 Java로 처리합니다.
        //(데이터가 매우 많아지면 이 부분은 네이티브 쿼리 최적화가 필요할 수 있습니다.)
        List<Pot> nearbyPots = filteredPots.getContent().stream()
                .filter(pot -> isWithinDistance(lat, lon, pot.getLatitude(), pot.getLongitude(), distance))
                .toList();

        //5.최종 결과를 DTO 페이지로 변환하여 반환합니다.
        Page<PotResponseDto> dtoPage = new PageImpl<>(
                nearbyPots.stream().map(this::createPotResponseDtoWithPresignedUrl).collect(Collectors.toList()),
                pageable,
                nearbyPots.size()
        );

        return dtoPage;
    }

    /**
     * 두 지점 사이의 거리를 계산하는 Haversine 공식 헬퍼 메서드
     */
    private boolean isWithinDistance(double lat1, double lon1, double lat2, double lon2, double distanceKm) {
        final int R = 6371; //지구의 반지름(km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = R * c; //km 단위 거리

        return distance <= distanceKm;
    }

    /**
     * Pot 엔티티를 받아 DTO를 생성하고, 이미지 URL이 존재하면 사전 서명된 URL을 생성하여 설정하는 헬퍼 메서드
     */
    public PotResponseDto createPotResponseDtoWithPresignedUrl(Pot pot) {
        PotResponseDto responseDto = new PotResponseDto(pot);
        if(pot.getImageUrl() != null && !pot.getImageUrl().isEmpty()) {
            responseDto.setImageUrl(generatePresignedUrl(pot.getImageUrl()));
        }

        return responseDto;
    }

    /**
     * 파일 키를 받아 사전 서명된 URL을 생성하는 헬퍼 메서드
     */
    private String generatePresignedUrl(String fileKey) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime() + (60 * 60 * 1000); //유효기간 1시간
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, fileKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL url = amazonS3Client.generatePresignedUrl(request);

        return url.toString();
    }
}
