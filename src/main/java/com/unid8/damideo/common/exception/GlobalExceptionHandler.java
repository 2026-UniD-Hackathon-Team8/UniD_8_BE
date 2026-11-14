package com.unid8.damideo.common.exception;

import com.unid8.damideo.common.response.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return toResponse(e.getErrorCode(), e.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return toResponse(CommonErrorCode.INVALID_INPUT, errors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        return toResponse(CommonErrorCode.INVALID_INPUT, Map.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        if ("X-Device-Id".equalsIgnoreCase(e.getHeaderName())) {
            return toResponse(CommonErrorCode.USER_NOT_FOUND, Map.of());
        }
        return toResponse(CommonErrorCode.INVALID_INPUT, Map.of(e.getHeaderName(), "필수 헤더가 없습니다."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return toResponse(CommonErrorCode.NOT_FOUND, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return toResponse(CommonErrorCode.INTERNAL_ERROR, Map.of());
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, Map<String, String> errors) {
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.fail(errorCode.code(), errorCode.message(), errors));
    }
}
