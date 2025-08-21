package com.subdivision.subdivision_prj.controller;

import com.amazonaws.Response;
import com.subdivision.subdivision_prj.dto.SignUpRequestDto;
import com.subdivision.subdivision_prj.service.AuthService;
import com.subdivision.subdivision_prj.dto.LoginRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController //이 클래스가 Restful API의 컨트롤러임을 나타냅니다. @ResponseBody가 포함되어 있어 객체를 JSON으로 반환합니다.
@RequestMapping("/api/auth") //이 컨트롤러의 모든 메서드 '/api/auth'라는 공통 경로를 가집니다.
@RequiredArgsConstructor //서비스 계층과 마찬가지로 의존성 주입을 위해 사용합니다.
public class AuthController {

    private final AuthService authService;

    // @PostMapping: HTTP POST 요청을 처리하는 메서드임을 나타냅니다.
    // "/signup"경로에 대한 요청을 이 메서드가 담당합니다. (최종 경로는 /api/auth/signup)
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequestDto requestDto) {
        // @RequestBody: HTTP 요청의 본문(body)에 담겨 오는 JSON 데이터를 SignUpRequestDto 객체를 변환해줍니다.

        // authService의 signup 메서드를 호출하여 회원가입 로직을 수행합니다.
        authService.signup(requestDto);

        // 회원가입 성공 시, HTTP 상태 코드 200(OK)와 함께 성공 메시지를 담아 응답합니다.
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto requestDto) {
        String token = authService.login(requestDto);

        return ResponseEntity.ok(token);
    }

    /**
     * 닉네임 중복 체크 API
     * @param nickname  중복 확인할 닉네임
     * @return 중복 여부를 담은 응답
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = authService.checkNicknameDuplicate(nickname);

        return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
    }

    /**
     * 이메일 중복 체크 API
     * @param email 중복 확인할 이메일
     * @return 중복 여부를 담은 응답
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);

        return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
    }
}
