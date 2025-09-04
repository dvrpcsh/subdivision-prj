package com.subdivision.subdivision_prj.config;

import com.subdivision.subdivision_prj.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security의 핵심 설정을 담당하는 클래스입니다.
 * 웹 애플리케이션의 보안(인증 및 인가)을 상세하게 제어합니다.
 */
@Configuration // 이 클래스가 Spring의 설정 파일임을 나타냅니다.
@EnableWebSecurity // Spring Security를 활성화하고 웹 보안 설정을 커스터마이징할 수 있게 합니다.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder를 Spring 컨테이너에 Bean으로 등록합니다.
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt는 현재 가장 널리 사용되는 안전한 해싱 알고리즘 중 하나로, 비밀번호를 안전하게 암호화합니다.
        return new BCryptPasswordEncoder();
    }

    /**
     * 모든 환경(local, prod)에 적용될 SecurityFilterChain을 설정합니다.
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS(Cross-Origin Resource Sharing) 설정을 활성화하고, 아래 정의된 corsConfigurationSource Bean을 사용합니다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF(Cross-Site Request Forgery) 공격 방어 기능을 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)

                // Spring Security가 세션을 생성하거나 사용하지 않도록 설정합니다. (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT 인증과 충돌할 수 있는 Spring Security의 기본 로그인 폼과 HTTP Basic 인증을 비활성화합니다.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 각 HTTP 요청에 대한 접근 권한을 세밀하게 설정합니다.
                .authorizeHttpRequests(authz -> authz
                        // CORS Preflight 요청(OPTIONS 메서드)은 인증 상태와 관계없이 항상 허용합니다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 회원가입, 로그인, OAuth2 관련 경로는 인증 없이 누구나 접근할 수 있도록 허용합니다.
                        .requestMatchers("/", "/api/auth/**", "/oauth2/**").permitAll()

                        // 인증 없이도 조회 가능한 공개 API 경로들을 허용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/pots/public", "/api/pots/search").permitAll()

                        // 웹소켓 연결을 위한 경로는 인증 없이 허용합니다.
                        .requestMatchers("/ws-chat/**").permitAll()

                        // 이미지 업로드 경로는 인증된 사용자만 접근 가능하도록 설정합니다.
                        .requestMatchers("/api/images/upload").authenticated()

                        // 헬스체크 경로를 인증없이 가능하도록 설정
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/health").permitAll()

                        // 위에서 명시적으로 허용한 경로 외의 모든 요청은 반드시 인증(로그인)을 거쳐야만 접근할 수 있습니다.
                        .anyRequest().authenticated()
                )
                // 직접 구현한 JwtAuthenticationFilter를 Spring Security의 기본 인증 필터(UsernamePasswordAuthenticationFilter) 앞에 배치합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // OAuth2 소셜 로그인 기능을 활성화합니다.
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 정책을 상세하게 설정하는 Bean입니다.
     * 다른 도메인에서 오는 API 요청을 허용하기 위해 반드시 필요합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 💡 [최종 수정] 요청을 허용할 출처(Origin) 목록을 설정합니다.
        // 로컬 개발 환경(localhost:3000)과 실제 운영 환경(https://www.dongne-gonggu.shop)을 모두 허용합니다.
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://www.dongne-gonggu.shop"));

        // 요청에서 허용할 HTTP 헤더를 설정합니다. ("*")는 모든 헤더를 의미합니다.
        configuration.addAllowedHeader("*");

        // 요청에서 허용할 HTTP 메서드(GET, POST 등)를 설정합니다. ("*")는 모든 메서드를 의미합니다.
        configuration.addAllowedMethod("*");

        // 브라우저가 자격 증명 정보(예: 쿠키, Authorization 헤더)를 요청에 포함하는 것을 허용합니다.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 위 CORS 정책을 적용합니다.
        return source;
    }
}

