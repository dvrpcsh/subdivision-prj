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
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 사용자 이메일과 권한 정보를 기반으로 JWT를 생성하는 메서드
     * @param userEmail 사용자 이메일
     * @return 생성된 JWT 문자열
     */
    public String createToken(String userEmail) {

        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs); // 만료 시간 설정

        // Jwts.builder()에 직접 subject를 설정하여 코드를 간소화
        return Jwts.builder()
                .subject(userEmail) // 토큰의 주체(subject)로 이메일을 설정
                .claim("roles", Collections.singletonList("ROLE_USER")) //'roles'정보를 claim으로 추가
                .issuedAt(now)      // 토큰 발행 시간 설정
                .expiration(validity) // 토큰 만료 시간 설정
                .signWith(key, SignatureAlgorithm.HS256) // 사용할 암호화 알고리즘과 비밀키 설정
                .compact();         // JWT 문자열 생성
    }

    /**
     * 토큰에서 사용자 이메일(주체)을 추출하는 메서드
     * @param token JWT
     * @return 사용자 이메일
     */
    public String getUserEmail(String token) {
        // Jwts.parser()를 사용하고, verifyWith(key)로 서명을 검증합니다.
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload() // Claims 대신 Payload를 가져옵니다.
                .getSubject();
    }

    /**
     * 토큰을 기반으로 Spring Security의 Authentication 객체를 생성하는 메서드
     * @param token JWT
     * @return Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        //토큰에서 이메일을 기반으로 UserDetails 객체를 로드합니다.
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserEmail(token));

        //UserDetails와 권한 정보를 포함하는 Authentication 객체를 반환합니다.
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 토큰의 유효성을 검증하는 메서드
     * @param token JWT
     * @return 토큰이 유효하면 true
     */
    public boolean validationToken(String token) {
        try {
            // Jwts.parser()로 변경하고, 예외 타입을 더 구체적으로 잡을 수 있습니다.
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }

        return false;
    }
}
