package com.unid8.damideo.game;

import com.unid8.damideo.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GameErrorCode implements ErrorCode {

    GAME_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 판을 찾을 수 없습니다."),
    GAME_SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 판에 접근할 권한이 없습니다."),
    GAME_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "이미 종료되었거나 중단된 판입니다."),
    GAME_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "아직 완료되지 않은 판입니다."),
    GAME_QUESTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 판에 포함된 문제가 아닙니다."),
    NO_ACTIVE_QUESTIONS(HttpStatus.INTERNAL_SERVER_ERROR, "출제할 수 있는 활성 문제가 없습니다.");

    private final HttpStatus status;
    private final String message;

    GameErrorCode(HttpStatus status, String message) {
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
