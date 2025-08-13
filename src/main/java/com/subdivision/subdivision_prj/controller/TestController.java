package com.subdivision.subdivision_prj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/me")
    public ResponseEntity<String> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        //@AuthenticationPrincipal 어노테이션을 통해 현재 인증된 사용자의 정보를 받아옵니다.
        //우리는 UserDetails의 username에 이메일을 저장했으므로, getUsername()으로 이메일을 가져올 수 있습니다.
        return ResponseEntity.ok("Your email is: " + userDetails.getUsername());
    }
}
