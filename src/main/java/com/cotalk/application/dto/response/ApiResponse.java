package com.cotalk.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 공통 API 응답 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
