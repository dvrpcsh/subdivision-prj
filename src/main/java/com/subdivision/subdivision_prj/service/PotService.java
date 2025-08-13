package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.Pot;
import com.subdivision.subdivision_prj.domain.PotRepository;
import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
