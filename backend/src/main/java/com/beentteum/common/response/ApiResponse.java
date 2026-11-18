
package com.beentteum.common.response;

public record ApiResponse<T>(
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("요청이 성공적으로 처리되었습니다.", data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(message, null);
    }
}
