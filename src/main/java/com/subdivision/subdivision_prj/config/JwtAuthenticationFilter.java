package com.subdivision.subdivision_prj.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security의 필터 체인에서 가장 먼저 동작하여 모든 요청에 대해 JWT 토큰을 검증하는 커스텀 필터입니다.
 * @author subdivision
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 모든 HTTP 요청이 컨트롤러에 도달하기 전에 이 메서드를 거칩니다.
     * @param request  들어오는 HttpServletRequest
     * @param response 나가는 HttpServletResponse
     * @param filterChain 다음 필터를 호출하기 위한 FilterChain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. CORS Preflight 요청(OPTIONS)은 토큰 검증 없이 즉시 통과시킵니다.
        // 이 로직이 없으면 프론트엔드에서 보내는 복잡한 요청(예: 이미지 업로드)이 CORS 정책에 의해 차단될 수 있습니다.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 요청 헤더에서 'Authorization' 헤더를 찾아 JWT 토큰을 추출합니다.
        String token = resolveToken(request);

        try {
            // 3. 토큰이 존재하고 유효한 경우에만 인증 절차를 진행합니다.
            if (token != null && jwtTokenProvider.validationToken(token)) {
                // 토큰이 유효하면, 토큰에서 사용자 정보(Authentication 객체)를 가져옵니다.
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                // SecurityContextHolder에 인증 정보를 저장합니다.
                // 이 과정을 통해, 이후의 보안 로직이나 컨트롤러에서 @AuthenticationPrincipal 등으로 현재 로그인한 사용자 정보에 접근할 수 있게 됩니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 토큰 유효성 검증 과정에서 예외가 발생하더라도 요청을 중단시키지 않고, 로그만 남깁니다.
            // 인증 정보가 설정되지 않았으므로, 해당 요청은 '인증되지 않은' 요청으로 처리됩니다.
            log.error("사용자 인증 정보를 설정할 수 없습니다: {}", e.getMessage());
        }

        // 4. 모든 처리가 끝나면, 다음 필터로 요청과 응답을 전달합니다.
        filterChain.doFilter(request, response);
    }

    /**
     * HttpServletRequest의 'Authorization' 헤더에서 'Bearer ' 접두사를 제거하고 순수한 토큰 문자열만 추출하는 헬퍼 메서드입니다.
     * @param request HttpServletRequest 객체
     * @return 추출된 토큰 문자열, 또는 토큰이 없거나 형식이 올바르지 않으면 null을 반환합니다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

