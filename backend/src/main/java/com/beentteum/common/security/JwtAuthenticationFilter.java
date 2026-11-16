package com.beentteum.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청의 Authorization: Bearer {token} 을 검증해 SecurityContext 에 인증 정보를 넣는다.
 * <p>
 * 토큰이 없거나 유효하지 않으면 인증 없이 다음 필터로 넘긴다.
 * 이후 인증이 필요한 Endpoint 라면 SecurityConfig 의 EntryPoint 가 401 을 응답한다.
 * <p>
 * SecurityConfig 에서 직접 생성한다(@Component 로 등록하면 서블릿 필터로 중복 등록됨).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                SecurityContextHolder.getContext().setAuthentication(jwtTokenProvider.getAuthentication(token));
            } catch (JwtException e) {
                // TODO(common-error): 공통 Error 규격 확정 시 만료(ExpiredJwtException)/위조 구분 응답이 필요하면
                //  request attribute 로 원인을 넘기고 AuthenticationEntryPoint 에서 처리한다.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    /** "Authorization: Bearer xxx" 에서 xxx 추출. 없거나 형식이 다르면 null */
    static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
