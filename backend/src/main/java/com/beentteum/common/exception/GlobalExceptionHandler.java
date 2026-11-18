
package com.beentteum.common.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 비즈니스 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException exception) {

        ErrorCode errorCode = exception.getErrorCode();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                errorCode.getStatus(),
                exception.getMessage()
        );

        problem.setProperty("code", errorCode.getCode());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(problem);
    }

    // 입력값 검증 실패: 400
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        List<ValidationError> errors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(ValidationError::from)
                        .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                errorCode.getStatus(),
                errorCode.getMessage()
        );

        problem.setProperty("code", errorCode.getCode());
        problem.setProperty("errors", errors);

        return ResponseEntity
                .status(errorCode.getStatus())
                .headers(headers)
                .body(problem);
    }

    // 잘못된 JSON 요청: 400
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "요청 본문을 읽을 수 없습니다."
        );

        problem.setProperty("code", "INVALID_REQUEST_BODY");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(problem);
    }

    // Spring MVC 기본 오류는 원래 HTTP 상태 코드를 유지한다.
    // 예: 404, 405, 415, 필수 파라미터 누락(400)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        if (status.is5xxServerError()) {
            log.error("Spring MVC 요청 처리 중 서버 오류가 발생했습니다.", exception);
        }

        return super.handleExceptionInternal(
                exception, body, headers, status, request
        );
    }
}
