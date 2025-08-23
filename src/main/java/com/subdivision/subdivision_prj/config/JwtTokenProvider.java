package com.subdivision.subdivision_prj.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

@Component //Spring 컨테이너에 Bean으로 등록
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    // application.properties에서 JWT 비밀키 값을 가져옵니다.
    @Value("${jwt.secret}")
    private String secretKey;

    // application.properties에서 JWT 만료 시간을 가져옵니다.
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key;
    // UserDetailsService 주입
    private final UserDetailsService userDetailsService;

    // 의존성 주입 후 초기화를 수행하는 메서드. 비밀키를 Key 객체로 변환합니다.
    @PostConstruct
    protected void init() {
        try{
            //비밀키 길이 검증(HS256은 최소 32바이트 필요)
            if(secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("JWT 비밀키가 null이거나 비었습니다.");
            }

            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

            if(keyBytes.length < 32) {
                log.warn("JWT 비밀키가 너무 짧습니다. 현재 길이: {}바이트, 권장 길이: 32바이트 이상");
                //비밀키를 32바이트로 패딩하거나 해시 처리
                StringBuilder paddedKey = new StringBuilder(secretKey);
                while(paddedKey.toString().getBytes(StandardCharsets.UTF_8).length < 32) {
                    paddedKey.append(secretKey);
                }
                keyBytes = paddedKey.toString().getBytes(StandardCharsets.UTF_8);

                if(keyBytes.length > 32) {
                    byte[] truncatedKey = new byte[32];
                    System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
                    keyBytes = truncatedKey;
                }
            }

            key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JwtTokenProvider 초기화 완료. 만료시간: {}ms", expirationMs);
        } catch(Exception e) {
            log.error("JWT 초기화 중 오류 발생: ", e);
            throw new RuntimeException("JWT 설정 초기화 실패", e);
        }
        //key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 사용자 이메일과 권한 정보를 기반으로 JWT를 생성하는 메서드
     * @param userEmail 사용자 이메일
     * @return 생성된 JWT 문자열
     */
    public String createToken(String userEmail) {

        //입력 값 검증
        if(userEmail == null || userEmail.trim().isEmpty()) {
            log.error("JWT 생성 실패: 사용자 이메일이 null 이거나 비어있습니다.");
            throw new IllegalArgumentException("사용자 이메일이 null 이거나 비어있습니다.");
        }

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + expirationMs); // 만료 시간 설정

            // Jwts.builder()에 직접 subject를 설정하여 코드를 간소화
            String token = Jwts.builder()
                    .subject(userEmail) // 토큰의 주체(subject)로 이메일을 설정
                    .claim("roles", Collections.singletonList("ROLE_USER")) //'roles'정보를 claim으로 추가
                    .issuedAt(now)      // 토큰 발행 시간 설정
                    .expiration(validity) // 토큰 만료 시간 설정
                    .signWith(key, SignatureAlgorithm.HS256) // 사용할 암호화 알고리즘과 비밀키 설정
                    .compact();         // JWT 문자열 생성

            log.info("JWT 토큰 생성 성공 - Email: {}", userEmail);
            return token;
        } catch(Exception e) {
            log.error("JWT 토큰 생성 중 오류 발생 - Email: {}, Error: ", userEmail, e);
            throw new RuntimeException("JWT 토큰 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 토큰에서 사용자 이메일(주체)을 추출하는 메서드
     * @param token JWT
     * @return 사용자 이메일
     */
    public String getUserEmail(String token) {

        if (token == null || token.trim().isEmpty()) {
            log.error("토큰이 null이거나 비어있습니다.");
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();

            String email = claims.getSubject();
            log.debug("토큰에서 이메일 추출 성공: {}", email);
            return email;

        } catch (Exception e) {
            log.error("토큰에서 사용자 이메일 추출 실패: ", e);
            return null;
        }
    }

    /**
     * 토큰을 기반으로 Spring Security의 Authentication 객체를 생성하는 메서드
     * @param token JWT
     * @return Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        try {
            String userEmail = this.getUserEmail(token);
            if (userEmail == null) {
                log.error("토큰에서 이메일을 추출할 수 없습니다.");
                return null;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());

        } catch (Exception e) {
            log.error("Authentication 객체 생성 실패: ", e);
            return null;
        }
    }

    /**
     * 토큰의 유효성을 검증하는 메서드
     * @param token JWT
     * @return 토큰이 유효하면 true
     */
    public boolean validationToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("토큰이 null이거나 비어있습니다.");
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.trim());
            return true;

        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn("잘못된 JWT 서명: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있거나 잘못됨: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류: ", e);
        }

        return false;
    }

    /**
     * 토큰에서 만료시간을 추출하는 메서드
     * @param token JWT
     * @return 만료시간 (Date)
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("토큰에서 만료시간 추출 실패: ", e);
            return null;
        }
    }

    /**
     * 토큰이 곧 만료되는지 확인하는 메서드
     * @param token JWT
     * @param beforeMinutes 몇 분 전부터 곧 만료로 간주할지
     * @return 곧 만료되면 true
     */
    public boolean isTokenExpiringSoon(String token, int beforeMinutes) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return true;
            }

            Date now = new Date();
            long timeDiff = expiration.getTime() - now.getTime();
            long minutesDiff = timeDiff / (60 * 1000);

            return minutesDiff <= beforeMinutes;
        } catch (Exception e) {
            log.error("토큰 만료 시간 확인 실패: ", e);
            return true;
        }
    }
}
