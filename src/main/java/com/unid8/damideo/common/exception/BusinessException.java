package com.unid8.damideo.common.exception;

import java.util.Map;
import lombok.Getter;

/**
 * 서비스 로직에서 정해진 에러 코드로 응답할 때 던지는 예외.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> errors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public BusinessException(ErrorCode errorCode, Map<String, String> errors) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.errors = errors;
    }
}
