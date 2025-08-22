package com.subdivision.subdivision_prj.config;

import com.subdivision.subdivision_prj.config.JwtTokenProvider; // JwtTokenProvider 경로 확인
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Slf4j import 추가
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler; // 상속 클래스 변경
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j // 로그 출력을 위해 추가
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler { // 상속 클래스 변경

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공! JWT 생성을 시작합니다.");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        String jwt = jwtTokenProvider.createToken(email);
        log.info("JWT 생성 성공: {}", jwt);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", jwt)
                .build().toUriString();

        // 응답이 이미 커밋되었는지 확인
        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되었습니다. 리다이렉트 불가: " + targetUrl);
            return;
        }

        // 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
        log.info("프런트엔드로 리다이렉트 수행: {}", targetUrl);
    }
}