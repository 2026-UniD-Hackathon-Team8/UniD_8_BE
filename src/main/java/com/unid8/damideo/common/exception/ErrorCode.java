package com.unid8.damideo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드 enum이 구현하는 인터페이스.
 * 각 도메인은 자기 패키지에 enum을 만들어 이 인터페이스를 구현한다.
 */
public interface ErrorCode {

    HttpStatus status();

    String code();

    String message();
}
