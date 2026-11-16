package com.beentteum.common.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정.
 * <p>
 * 접근 규칙 (위에서부터 순서대로 매칭):
 * <ol>
 *   <li>{@link #PUBLIC_ENDPOINTS}: 인증 없이 허용</li>
 *   <li>/api/admin/**: ADMIN 만 허용</li>
 *   <li>그 외 모든 요청: 인증 필요 (USER, ADMIN 모두 가능)</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 인증 없이 접근 가능한 경로.
     * - /api/auth/** : 로그인/회원가입/토큰 재발급 등 (auth 도메인)
     * - /error       : Spring Boot 기본 에러 경로. 막으면 404/500 이 401 로 가려진다.
     * 비로그인 조회 API(예: 카페 목록)가 확정되면 여기에 추가한다.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/error",
    };

    private static final String ADMIN_ENDPOINTS = "/api/admin/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider)
            throws Exception {
        http
                // JWT 를 Authorization 헤더로 전달(쿠키 미사용)하므로 CSRF 불필요
                .csrf(AbstractHttpConfigurer::disable)
                // 아래 corsConfigurationSource Bean 사용
                .cors(Customizer.withDefaults())
                // 세션 미사용(Stateless). 매 요청 JWT 로 인증
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // REST API 이므로 기본 로그인 폼/Basic 인증/로그아웃 페이지 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated())
                // 미인증 401, 권한 부족 403(기본 AccessDeniedHandler).
                // TODO(common-error): 공통 Error 규격 확정 시 응답 Body 를 해당 규격으로 작성하는
                //  AuthenticationEntryPoint / AccessDeniedHandler 로 교체한다.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // 토큰을 헤더로 보내므로 쿠키 전송(credentials) 불필요
        config.setAllowCredentials(false);
        // Preflight 결과 캐시(초)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
