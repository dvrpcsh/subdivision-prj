package com.subdivision.subdivision_prj.config;

import com.subdivision.subdivision_prj.config.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${oauth2.redirect.base-uri:https://www.dongne-gonggu.shop}")
    private String redirectBaseUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공! JWT 생성을 시작합니다.");

        try {

            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            //1.provider(google, kakao, naver)정보를 가져옵니다.
            String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            log.info("Provider: {}", provider);

            String email = null;
            String id = null;

            //2,provider별로 분기하여 이메일을 정확히 추출합니다.
            if (provider.equals("google")) {
                id = oAuth2User.getAttribute("sub");
                email = oAuth2User.getAttribute("email");
                log.info("Google - ID: {}, Email: {}", id, email);
            } else if (provider.equals("kakao")) {
                Object kakaoIdObj = oAuth2User.getAttribute("id");
                if (kakaoIdObj != null) {
                    id = String.valueOf(kakaoIdObj);
                }
                log.info("Kakao - ID Object Type: {}, ID: {}",
                        kakaoIdObj != null ? kakaoIdObj.getClass().getSimpleName() : "null", id);

                // 카카오 계정 정보에서 이메일 추출
                Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                    log.info("Kakao - Email from kakao_account: {}", email);
                }
            } else if (provider.equals("naver")) {
                Map<String, Object> responseMap = oAuth2User.getAttribute("response");
                if (responseMap != null) {
                    id = (String) responseMap.get("id");
                    email = (String) responseMap.get("email");
                }
                log.info("Naver - ID: {}, Email: {}", id, email);
            }

            // 3. ID가 없는 경우 오류 처리
            if (id == null || id.trim().isEmpty()) {
                log.error("사용자 ID를 가져올 수 없습니다. Provider: {}", provider);
                handleAuthenticationFailure(response, "사용자 정보를 가져올 수 없습니다.");
                return;
            }

            // 4. 이메일이 없는 경우를 대비해 임시 이메일을 생성합니다.
            if (email == null || email.trim().isEmpty()) {
                email = id + "@" + provider + ".com";
                log.info("임시 이메일 생성: {}", email);
            }

            // 5. JWT 토큰 생성
            log.info("JWT 토큰 생성 시작 - Email: {}", email);
            String jwt = jwtTokenProvider.createToken(email);
            log.info("JWT 생성 성공");

            // 6. 리다이렉트 URL 구성 - 환경 변수 사용
            String targetUrl = UriComponentsBuilder.fromUriString(redirectBaseUri + "/oauth2/redirect")
                    .queryParam("token", jwt)
                    .build().toUriString();

            // 7. 응답이 이미 커밋되었는지 확인
            if (response.isCommitted()) {
                log.debug("응답이 이미 커밋되었습니다. 리다이렉트 불가: " + targetUrl);
                return;
            }

            // 8. 리다이렉트 수행
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            log.info("프런트엔드로 리다이렉트 수행: {}", targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: ", e);
            handleAuthenticationFailure(response, "로그인 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        String errorUrl = UriComponentsBuilder.fromUriString(redirectBaseUri + "/oauth2/redirect")
                .queryParam("error", message)
                .build().toUriString();

        if (!response.isCommitted()) {
            response.sendRedirect(errorUrl);
        }
    }
}