package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.Pot;
import com.subdivision.subdivision_prj.domain.PotRepository;
import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final UserRepository userRepository;
    private final PotRepository potRepository;

    /**
     * 현재 로그인 한 사용자가 작성한 모든 팟 목록을 조회합니다.
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 해당 사용자가 작성한 팟의 DTO 목록
     */
    public List<PotResponseDto> getMyPots(UserDetails userDetails) {
        //UserDetails에서 이메일을 가져와 현재 User 엔티티를 조회합니다.
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //PotRepository를 사용하여 해당 User가 작성한 모든 Pot을 조회합니다.
        List<Pot> myPots = potRepository.findAllByUser(currentUser);

        //조회된 Pot 엔티티 리스트를 PotResponseDto 리스트로 변환하여 반환합니다.
        return myPots.stream()
                .map(PotResponseDto::new)
                .collect(Collectors.toList());
    }
}
