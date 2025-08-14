package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.Pot;
import com.subdivision.subdivision_prj.domain.PotRepository;
import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줍니다
public class PotService {

    private final PotRepository potRepository;
    private final UserRepository userRepository;

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

        //4.저장된 Pot 엔티티를 PotResponseDto로 변환하여 반환합니다.
        return new PotResponseDto(savedPot);
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

        return new PotResponseDto(pot);
    }

    /**
     * 모든 팟의 목록을 조회하는 메서드
     * @return 모든 팟의 정보를 담은 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PotResponseDto> getAllPots() {
        return potRepository.findAll().stream()
                .map(PotResponseDto::new) //pot -> new PotResponseDto(pot)와 동일
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
        pot.update(requestDto.getTitle(), requestDto.getContent());

        //3.변경된 팟 정보를 DTO로 변환하여 반환
        //@Transactional에 의해 메서드가 끝나면 변경된 내용이 자동으로 DB에 반영됩니다.(Dirty Checking)
        /*
         * Dirty Checking: @Transactional이 적용된 메서드 안에서 JPA가 관리하는 엔티티의 상태가 변경되면,
         * 이 메서드가 끝낼 때 JPA가 이 변경을 감지하여 자동으로 UPDATE 쿼리를 실행해줍니다.
         * 따라서 potRepository.save()를 다시 호출할 필요가 없습니다.
         */
        return new PotResponseDto(pot);
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
}
