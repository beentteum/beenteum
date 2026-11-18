
package com.beentteum.common.exception;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException exception) {

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
                .body(problem);
    }

    // 잘못된 JSON 요청: 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleInvalidJson(
            HttpMessageNotReadableException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "요청 본문을 읽을 수 없습니다."
        );

        problem.setProperty("code", "INVALID_REQUEST_BODY");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }

    // 지원하지 않는 HTTP 메서드: 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "지원하지 않는 HTTP 메서드입니다."
        );

        problem.setProperty("code", "METHOD_NOT_ALLOWED");

        HttpMethod[] supportedMethods = Arrays.stream(
                        exception.getSupportedMethods() == null
                                ? new String[0]
                                : exception.getSupportedMethods()
                )
                .map(HttpMethod::valueOf)
                .toArray(HttpMethod[]::new);

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(supportedMethods)
                .body(problem);
    }

    // 예상하지 못한 서버 오류: 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception) {

        log.error("예상하지 못한 서버 오류가 발생했습니다.", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                errorCode.getStatus(),
                errorCode.getMessage()
        );

        problem.setProperty("code", errorCode.getCode());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(problem);
    }
}
