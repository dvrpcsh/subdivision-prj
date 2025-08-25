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

import javax.swing.plaf.IconUIResource;

@Configuration // 이 클래스가 Spring의 설정 파일임을 나타냅니다.
@EnableWebSecurity // Spring Security를 활성화합니다.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

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
                //CORS설정을 HttpSecurity에 통합합니다.
                //이 설정을 통해 아래 corsConfigurationSource() Bean을 Security Filter Chain에서 사용하게됩니다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                //CSRF(Cross-Site Request Forgery) 공격 방어 기능을 비활성화합니다.
                //REST API 서버는 세션을 사용하지 않고 토큰 기반 인증(JWT)을 사용하므로 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable)

                //세션을 상태 없이(stateless) 관리하도록 설정합니다. 이 역시 JWT인증을 위한 설정입니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //HTTP 요청에 대한 접근 권한을 설정합니다.
                .authorizeHttpRequests(authz -> authz
                        //Preflight요청(OPTIONS)은 인증 없이 무조건 허용합니다.
                        //이것이 CORS에러를 해결하는 핵심 부분입니다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // "/" (메인 페이지), "/api/auth/**" (회원가입/로그인) 경로는 인증 없이 누구나 접근할 수 있도록 허용합니다.
                        .requestMatchers("/", "/api/auth/**").permitAll()

                        // GET 요청에 대해 모두 허용
                        .requestMatchers(HttpMethod.GET, "/api/pots/public", "/api/pots/search").permitAll()

                        //이미지 업로드 경로는 테스트를 위해 임시로 모두 허용
                        .requestMatchers("/api/images/upload").authenticated()

                        // 그 외의 모든 요청은 반드시 인증(로그인)을 거쳐야만 접근할 수 있도록 설정합니다.
                        .anyRequest().authenticated()
                )
                //우리가 만든 JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                //OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService) //사용자 정보를 처리할 서비스
                    )
                    .successHandler(oAuth2LoginSuccessHandler) //로그인 성공 후 처리할 핸들러
        );

        return http.build();
    }

    /**
     * CORS 정책을 상세하게 설정하는 Bean입니다.
     * WebConfig에서 분리하여 SecurityConfig에 통합 관리합니다.
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CORS 정책을 설정하기 위한 객체 생성
        CorsConfiguration configuration = new CorsConfiguration();

        // 💡 [중요] 요청을 허용할 프론트엔드 서버의 Origin(출처)을 명시적으로 지정합니다.
        // React 개발 서버 주소인 "http://localhost:3000"를 추가합니다.
        configuration.addAllowedOrigin("http://localhost:3000");

        // 요청에서 허용할 HTTP 헤더를 설정합니다. "*"는 모든 헤더를 의미합니다.
        // "Authorization" 헤더를 포함한 모든 커스텀 헤더를 허용하기 위함입니다.
        configuration.addAllowedHeader("*");

        // 요청에서 허용할 HTTP 메서드(GET, POST 등)를 설정합니다. "*"는 모든 메서드를 의미합니다.
        // Preflight 요청인 OPTIONS 메서드도 포함됩니다.
        configuration.addAllowedMethod("*");

        // 💡 [중요] 브라우저가 자격 증명(Credentials) 정보(예: 쿠키, JWT 토큰)를 요청에 포함하는 것을 허용합니다.
        // 이 설정이 false이면 프론트에서 보낸 Authorization 헤더가 무시될 수 있습니다.
        configuration.setAllowCredentials(true);

        // 위에서 설정한 CORS 정책을 모든 URL 경로("/**")에 적용하기 위한 소스를 생성합니다.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
