package com.subdivision.subdivision_prj.controller;

import com.amazonaws.Response;
import com.subdivision.subdivision_prj.dto.UserResponseDto;
import com.subdivision.subdivision_prj.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 현재 로그인 한 사용자의 정보를 조회하는 API
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto myInfo = userService.getMyInfo(userDetails);

        return ResponseEntity.ok(myInfo);
    }
}
