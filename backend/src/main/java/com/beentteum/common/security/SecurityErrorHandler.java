package com.beentteum.common.security;

import com.beentteum.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security Filter 단계의 401/403 응답을 공통 오류 규격(RFC 9457 ProblemDetail + code)으로 작성한다.
 * <p>
 * Filter 에서 발생한 예외는 @RestControllerAdvice(GlobalExceptionHandler)까지 전달되지 않으므로
 * Security 전용 핸들러가 직접 응답을 쓴다. 응답 형태는 GlobalExceptionHandler 와 동일하다.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public SecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 401: 토큰 없음(UNAUTHORIZED) / 만료(EXPIRED_TOKEN) / 위조·형식 오류(INVALID_TOKEN) */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // JwtAuthenticationFilter 가 토큰 검증 실패 원인을 넘겨준다. 없으면 토큰 자체가 없는 요청.
        ErrorCode errorCode = request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE) instanceof ErrorCode e
                ? e
                : ErrorCode.UNAUTHORIZED;
        write(request, response, errorCode);
    }

    /** 403: 인증은 됐지만 권한 부족 (예: USER 가 /api/admin/**) */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, ErrorCode.ACCESS_DENIED);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", errorCode.getStatus().getReasonPhrase());
        body.put("status", errorCode.getStatus().value());
        body.put("detail", errorCode.getMessage());
        body.put("instance", request.getRequestURI());
        body.put("code", errorCode.getCode());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
