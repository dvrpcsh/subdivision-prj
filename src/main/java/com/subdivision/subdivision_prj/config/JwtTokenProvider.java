package com.subdivision.subdivision_prj.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component //Spring 컨테이너에 Bean으로 등록
@RequiredArgsConstructor
public class JwtTokenProvider {

    // application.properties에서 JWT 비밀키 값을 가져옵니다.
    @Value("${jwt.secret}")
    private String secretKey;

    // application.properties에서 JWT 만료 시간을 가져옵니다.
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private Key key;

    // 의존성 주입 후 초기화를 수행하는 메서드. 비밀키를 Key 객체로 변환합니다.
    @PostConstruct
    protected void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 사용자 이메일을 기반으로 JWT를 생성하는 메서드
     * @param userEmail 사용자 이메일
     * @return 생성된 JWT 문자열
     */
    public String createToken(String userEmail) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs); // 만료 시간 설정

        // Jwts.builder()에 직접 subject를 설정하여 코드를 간소화
        return Jwts.builder()
                .subject(userEmail) // Claims를 만드는 대신 바로 주체(subject)를 설정
                .issuedAt(now)      // 토큰 발행 시간 설정
                .expiration(validity) // 토큰 만료 시간 설정
                .signWith(key, SignatureAlgorithm.HS256) // 사용할 암호화 알고리즘과 비밀키 설정
                .compact();         // JWT 문자열 생성
    }
}
