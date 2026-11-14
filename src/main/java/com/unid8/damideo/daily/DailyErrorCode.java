package com.unid8.damideo.daily;

import com.unid8.damideo.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum DailyErrorCode implements ErrorCode {

    DAILY_QUESTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "오늘의 데일리 문제가 아닙니다."),
    DAILY_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "오늘의 데일리 5문제를 모두 풀어야 결과를 볼 수 있습니다."),
    INSUFFICIENT_DAILY_QUESTIONS(HttpStatus.INTERNAL_SERVER_ERROR, "데일리 문제를 구성할 활성 문제가 부족합니다.");

    private final HttpStatus status;
    private final String message;

    DailyErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }
}
