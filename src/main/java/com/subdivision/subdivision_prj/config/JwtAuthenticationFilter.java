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
import java.util.Enumeration;

/**
 * Spring Security의 필터 체인에서 가장 먼저 동작하는 커스텀 JWT 인증 필터입니다.
 * [최종 디버깅 버전] 문제 추적을 위해 상세한 로그를 추가했습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // ============================ [CCTV 로그 #1: 모든 요청 기록] ============================
        // 이 필터를 거치는 모든 요청의 메서드와 URI를 로그로 출력합니다.
        // 이미지 업로드 시 'OPTIONS /api/images/upload'와 'POST /api/images/upload'가 모두 보이는지 확인하기 위함입니다.
        log.info(">>> [CCTV] JWT Filter 진입: Method = {}, URI = {}", request.getMethod(), request.getRequestURI());
        // 요청에 포함된 모든 헤더를 출력하여 'Authorization' 헤더가 실제로 존재하는지 확인합니다.
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.info(">>> [CCTV] Header: {} = {}", headerName, request.getHeader(headerName));
            }
        }
        log.info(">>> [CCTV] =========================================================");
        // =================================================================================

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            log.info(">>> [CCTV] OPTIONS 요청이므로 토큰 검증 없이 통과시킵니다.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        try {
            if (token != null && jwtTokenProvider.validationToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info(">>> [CCTV] 인증 성공! Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
            } else {
                log.warn(">>> [CCTV] 토큰이 없거나 유효하지 않습니다. 인증을 설정하지 않고 다음 필터로 넘어갑니다.");
            }
        } catch (Exception e) {
            log.error(">>> [CCTV] 사용자 인증 설정 중 예외 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

