package com.subdivision.subdivision_prj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 이 클래스가 Spring의 설정 파일임을 나타냅니다.
@EnableWebSecurity // Spring Security를 활성화합니다.
public class SecurityConfig {

    //@Bean 어노테이션을 통해 이 메서드가 반환하는 객체(PasswordEncoder)를 Spring 컨테이너에 등록합니다.
    //이렇게 등록된 객체는 다른 곳에서 주입받아 사용할 수 있습니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        //Bcrypt는 강력한 해싱 알고리즘 중 하나로, 비밀번호를 안전하게 암호화합니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //CSRF(Cross-Site Request Forgery) 공격 방어 기능을 비활성화합니다.
                //REST API 서버는 세션을 사용하지 않고 토큰 기반 인증(JWT)을 사용하므로 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)

                //세션을 상태 없이(stateless) 관리하도록 설정합니다. 이 역시 JWT인증을 위한 설정입니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //HTTP 요청에 대한 접근 권한을 설정합니다.
                .authorizeHttpRequests(authz -> authz
                        // "/" (메인 페이지), "/api/auth/**" (회원가입/로그인) 경로는 인증 없이 누구나 접근할 수 있도록 허용합니다.
                        .requestMatchers("/", "api/auth/**").permitAll()
                        // 그 외의 모든 요청은 반드시 인증(로그인)을 거쳐야만 접근할 수 있도록 설정합니다.
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
