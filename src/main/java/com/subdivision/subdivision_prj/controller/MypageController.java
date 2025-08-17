package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.dto.PotResponseDto;
import com.subdivision.subdivision_prj.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mypage") //이 컨트롤러의 모든 API는 /api/mypage로 시작합니다.
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    /**
     * 현재 로그인 한 사용자가 작성한 모든 팟(Pot) 목록을 조회하는 API
     * @param userDetails JWT 인증 필터를 통해 얻은 현재 사용자 정보
     * @return 작성한 팟 목록과 HTTP 200 OK 상태 코드
     */
    @GetMapping("/my-pots")
    public ResponseEntity<List<PotResponseDto>> getMyPots(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        //MyPageService를 호출하여 비즈니스 로직을 수행합니다.
        List<PotResponseDto> myPots = mypageService.getMyPots(userDetails);

        return ResponseEntity.ok(myPots);
    }
}
