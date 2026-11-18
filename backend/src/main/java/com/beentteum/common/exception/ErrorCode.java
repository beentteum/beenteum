
package com.beentteum.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT_VALUE(
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT_VALUE",
            "잘못된 입력값입니다."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "인증이 필요합니다."
    ),

    EXPIRED_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "EXPIRED_TOKEN",
            "만료된 토큰입니다."
    ),

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN",
            "유효하지 않은 토큰입니다."
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "접근 권한이 없습니다."
    ),

    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다."
    ),

    RESOURCE_CONFLICT(
            HttpStatus.CONFLICT,
            "RESOURCE_CONFLICT",
            "요청한 리소스가 이미 존재합니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
