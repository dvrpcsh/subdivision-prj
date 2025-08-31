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
     * 애플리케이션의 메인 보안 필터 체인을 설정합니다.
     * 이제 local과 prod 환경 모두에서 OAuth2를 사용하므로, SecurityFilterChain을 하나로 통합하여 관리합니다.
     * @param http HttpSecurity 객체. 보안 설정을 구성하는 핵심 객체입니다.
     * @return 구성이 완료된 SecurityFilterChain 객체
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS(Cross-Origin Resource Sharing) 설정
                // 다른 도메인(e.g., http://localhost:3000)에서의 요청을 허용하기 위해 아래 정의된 corsConfigurationSource Bean을 사용합니다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. CSRF(Cross-Site Request Forgery) 비활성화
                // JWT와 같은 토큰 기반 인증을 사용하는 API 서버는 세션 상태에 의존하지 않으므로 CSRF 공격에 비교적 안전합니다.
                // 따라서 불필요한 CSRF 방어 로직을 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)

                // 3. 세션 관리 정책 설정 (Stateless)
                // 서버가 세션을 생성하거나 사용하지 않도록 설정합니다. 모든 인증은 요청 헤더의 JWT를 통해 이루어집니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. [핵심] 불필요한 기본 인증 방식 비활성화
                // JWT 인증과 충돌하여 403 에러나 로그인 페이지 HTML 응답을 유발할 수 있는 Spring Security의 기본 인증 방식을 비활성화합니다.
                .formLogin(AbstractHttpConfigurer::disable) // Form 기반 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화

                // 5. HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // CORS Preflight 요청(OPTIONS 메서드)은 인증 상태와 관계없이 항상 허용합니다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // [핵심] 회원가입, 로그인 뿐만 아니라 OAuth2 관련 경로도 인증 없이 누구나 접근할 수 있도록 허용합니다.
                        // 이 설정이 없으면 소셜 로그인을 시작조차 할 수 없습니다.
                        .requestMatchers("/", "/api/auth/**", "/oauth2/**").permitAll()

                        // 인증 없이도 조회 가능한 공개 API 경로들을 허용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/pots/public", "/api/pots/search").permitAll()

                        // 웹소켓 연결을 위한 경로는 인증 없이 허용합니다. (인증은 Stomp 핸들러에서 별도 처리 예정)
                        .requestMatchers("/ws-chat/**").permitAll()

                        // 위에서 명시적으로 허용한 경로 외의 모든 요청은 반드시 인증(로그인)을 거쳐야만 접근할 수 있습니다.
                        .anyRequest().authenticated()
                )
                // 6. 직접 구현한 JWT 필터 추가
                // 모든 요청이 컨트롤러에 도달하기 전에 JwtAuthenticationFilter를 먼저 거치도록 설정합니다.
                // 이를 통해 요청 헤더의 JWT 토큰을 검증하고 사용자 인증 정보를 설정합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 7. OAuth2 소셜 로그인 기능 활성화
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                // 소셜 로그인 성공 후 사용자 정보를 가져왔을 때, 후처리를 담당할 서비스를 등록합니다.
                                // (e.g., DB에 사용자 정보 저장 또는 업데이트)
                                .userService(customOAuth2UserService)
                        )
                        // 소셜 로그인 최종 성공 후, JWT 발급 등의 추가 작업을 처리할 핸들러를 등록합니다.
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 정책을 상세하게 설정하는 Bean입니다.
     * 다른 도메인(e.g., http://localhost:3000)에서 오는 API 요청을 허용하기 위해 반드시 필요합니다.
     * @return CorsConfigurationSource 객체
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 요청을 허용할 프론트엔드 서버의 Origin(출처)을 명시적으로 지정합니다.
        configuration.addAllowedOrigin("http://localhost:3000");

        // "Authorization" 헤더(JWT 토큰)를 포함한 모든 커스텀 헤더를 주고받을 수 있도록 허용합니다.
        configuration.addAllowedHeader("*");

        // GET, POST, PUT, DELETE 등 모든 HTTP 메서드를 허용합니다.
        configuration.addAllowedMethod("*");

        // 브라우저가 자격 증명 정보(예: 쿠키, Authorization 헤더)를 요청에 포함하는 것을 허용합니다.
        configuration.setAllowCredentials(true);

        // 위에서 설정한 CORS 정책을 모든 URL 경로("/**")에 적용합니다.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

