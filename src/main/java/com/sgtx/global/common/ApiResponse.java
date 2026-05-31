package com.sgtx.global.common;

public record ApiResponse<T>(
    int status,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
}
