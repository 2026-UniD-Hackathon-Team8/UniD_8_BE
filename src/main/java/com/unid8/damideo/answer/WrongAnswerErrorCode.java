package com.unid8.damideo.answer;

import com.unid8.damideo.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum WrongAnswerErrorCode implements ErrorCode {

    WRONG_ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "오답노트에 없는 문제입니다.");

    private final HttpStatus status;
    private final String message;

    WrongAnswerErrorCode(HttpStatus status, String message) {
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
