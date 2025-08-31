package com.subdivision.subdivision_prj.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

/**
 * JWT(Json Web Token)의 생성, 검증, 정보 추출 등 토큰 관련 모든 기능을 담당하는 핵심 클래스입니다.
 * @author subdivision
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // application.properties에서 JWT 비밀키(Base64 인코딩) 값을 주입받습니다.
    @Value("${jwt.secret}")
    private String secretKey;

    // application.properties에서 JWT 만료 시간(밀리초)을 주입받습니다.
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // JWT 서명에 사용할 최종 SecretKey 객체입니다.
    private SecretKey key;

    // DB에서 사용자 정보를 조회하기 위한 Spring Security의 UserDetailsService입니다.
    private final UserDetailsService userDetailsService;

    /**
     * 의존성 주입이 완료된 후, JWT 서명에 사용할 SecretKey를 초기화하는 메서드입니다.
     * @PostConstruct 어노테이션을 통해 애플리케이션 시작 시 한 번만 실행됩니다.
     */
    @PostConstruct
    protected void init() {
        log.info("JwtTokenProvider 초기화를 시작합니다...");
        try {
            if (secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("JWT 비밀키(jwt.secret)가 application.properties에 설정되지 않았습니다.");
            }
            // Base64로 인코딩된 비밀키를 디코딩하여 byte 배열로 변환합니다.
            // 이 방식은 문자 인코딩(UTF-8, EUC-KR 등)으로 인한 문제를 원천적으로 방지하는 가장 안전하고 표준적인 방법입니다.
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);

            // 디코딩된 byte 배열을 사용하여 HMAC-SHA 알고리즘에 맞는 SecretKey 객체를 생성합니다.
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT SecretKey 초기화 성공. 만료시간: {}ms", expirationMs);

        } catch (Exception e) {
            log.error("JWT SecretKey 초기화 중 심각한 오류 발생: {}", e.getMessage(), e);
            // JWT 키 초기화 실패는 보안상 심각한 문제이므로, 애플리케이션 실행을 중단시키는 것이 더 안전합니다.
            throw new RuntimeException("JWT SecretKey 초기화에 실패했습니다. Base64 인코딩 값을 확인하세요.", e);
        }
    }

    /**
     * 사용자 이메일을 기반으로 새로운 JWT를 생성합니다.
     * @param userEmail 토큰에 담을 사용자의 이메일 (토큰의 주체(subject)가 됨)
     * @return 생성된 JWT 문자열
     */
    public String createToken(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 생성을 위한 사용자 이메일은 비어있을 수 없습니다.");
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs); // 현재 시간 + 만료 시간

        return Jwts.builder()
                .subject(userEmail) // 토큰의 주체(subject)로 이메일을 설정
                .claim("roles", Collections.singletonList("ROLE_USER")) // 사용자 역할(Role) 정보를 Claim으로 추가
                .issuedAt(now)      // 토큰 발행 시간
                .expiration(validity) // 토큰 만료 시간
                .signWith(key) // 초기화된 SecretKey로 서명
                .compact();         // 최종적으로 JWT 문자열 생성
    }

    /**
     * 주어진 JWT를 복호화하여 Spring Security가 이해할 수 있는 Authentication 객체로 변환합니다.
     * @param token 검증할 JWT 문자열
     * @return 생성된 Authentication 객체, 실패 시 null 반환
     */
    public Authentication getAuthentication(String token) {
        // 토큰에서 사용자 이메일을 추출합니다.
        String userEmail = this.getUserEmail(token);
        if (userEmail == null) {
            return null;
        }
        // 추출된 이메일로 UserDetailsService를 통해 DB에서 사용자 정보를 조회합니다.
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        // 조회된 UserDetails를 기반으로 Authentication 객체를 생성하여 반환합니다.
        // 이 객체는 SecurityContextHolder에 저장되어, 애플리케이션 전반에서 인증된 사용자로 인식됩니다.
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 토큰에서 사용자 이메일(Subject)을 추출합니다.
     * @param token JWT 문자열
     * @return 추출된 사용자 이메일, 실패 시 null 반환
     */
    public String getUserEmail(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key) // 서명 검증
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject(); // Subject(사용자 이메일) 반환
        } catch (Exception e) {
            log.warn("토큰에서 사용자 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰의 유효성을 검증하는 메서드입니다. (서명, 만료일 등)
     * @param token 검증할 JWT 문자열
     * @return 토큰이 유효하면 true, 아니면 false
     */
    public boolean validationToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("토큰이 비어있습니다.");
            return false;
        }

        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException | MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 서명 또는 토큰입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 클레임 문자열이 비어있습니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류 발생: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 만료 시간을 추출합니다.
     * @param token JWT 문자열
     * @return 만료 시간(Date 객체), 실패 시 null
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (Exception e) {
            log.error("토큰에서 만료 시간 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰이 곧 만료되는지 확인하는 유틸리티 메서드입니다.
     * @param token JWT 문자열
     * @param beforeMinutes 만료 기준으로 삼을 시간(분)
     * @return 지정된 시간 내에 만료 예정이면 true
     */
    public boolean isTokenExpiringSoon(String token, int beforeMinutes) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return true; // 만료 시간을 읽을 수 없으면 만료된 것으로 간주
        }
        long timeDiff = expiration.getTime() - new Date().getTime();
        return timeDiff < (long) beforeMinutes * 60 * 1000;
    }
}

