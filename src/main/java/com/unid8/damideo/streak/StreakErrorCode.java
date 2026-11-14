package com.unid8.damideo.streak;

import com.unid8.damideo.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StreakErrorCode implements ErrorCode {

    NO_QUESTION_AVAILABLE(HttpStatus.CONFLICT, "낼 수 있는 문제가 없습니다."),
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "연승 챌린지를 찾을 수 없습니다."),
    CHALLENGE_ENDED(HttpStatus.CONFLICT, "이미 끝난 연승 챌린지입니다."),
    NOT_CURRENT_QUESTION(HttpStatus.BAD_REQUEST, "지금 풀 문제가 아닙니다.");

    private final HttpStatus status;
    private final String message;

    StreakErrorCode(HttpStatus status, String message) {
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
