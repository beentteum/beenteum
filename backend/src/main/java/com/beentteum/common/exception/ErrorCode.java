
package com.beentteum.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT_VALUE",
            "잘못된 입력값입니다."
    ),

    // 401 Unauthorized
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "인증이 필요합니다."
    ),

    // 403 Forbidden
    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "접근 권한이 없습니다."
    ),

    // 404 Not Found
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다."
    ),

    // 409 Conflict
    RESOURCE_CONFLICT(
            HttpStatus.CONFLICT,
            "RESOURCE_CONFLICT",
            "요청한 리소스가 이미 존재합니다."
    ),

    // 500 Internal Server Error
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
