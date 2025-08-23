package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.*;
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
    private final PotMemberRepository potMemberRepository;
    private final PotService potService;

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
                .map(potService::createPotResponseDtoWithPresignedUrl)
                .collect(Collectors.toList());
    }

    /**
     * 현재 로그인 한 사용자가 참여한 모든 팟 목록을 조회합니다.
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 해당 사용자가 참여한 팟의 DTO 목록
     */
    public List<PotResponseDto> getJoinedPots(UserDetails userDetails) {
        //현재 User 엔티티를 조회합니다.
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //PotMemberRepository를 사용하여 해당 User의 모든 참여 기록(PotMember)을 조회합니다.
        List<PotMember> joinedMemberships = potMemberRepository.findAllByUser(currentUser);

        //각 참여 기록(PotMember)에서 Pot 정보만 추출하여 DTO 리스트로 변환 후 반환합니다.
        return joinedMemberships.stream()
                .map(potMember -> potService.createPotResponseDtoWithPresignedUrl(potMember.getPot()))
                .collect(Collectors.toList());
    }

    /**
     * 사용자 닉네임을 변경하는 서비스 로직
     */
    public void updateNickname(UserDetails userDetails, String newNickname) {
        // 1. 현재 사용자 정보를 가져옵니다.
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 새로운 닉네임이 이미 존재하는지 확인합니다.
        if (userRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 3. 사용자 닉네임을 업데이트합니다.
        currentUser.updateNickname(newNickname);
        userRepository.save(currentUser); // @Transactional에 의해 자동 저장되지만, 명시적으로 호출
    }
}
