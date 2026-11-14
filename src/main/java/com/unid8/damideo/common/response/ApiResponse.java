package com.unid8.damideo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * 모든 API 응답의 공통 형식.
 * 성공: success, code, message, data
 * 실패: success, code, message, errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Map<String, String> errors
) {

    public static <T> ApiResponse<T> ok(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, null);
    }

    public static ApiResponse<Void> fail(String code, String message, Map<String, String> errors) {
        return new ApiResponse<>(false, code, message, null, errors == null ? Map.of() : errors);
    }
}
