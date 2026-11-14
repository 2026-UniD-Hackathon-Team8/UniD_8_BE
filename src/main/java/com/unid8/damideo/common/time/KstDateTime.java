package com.unid8.damideo.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 서버 기준 시각 유틸.
 * DB 저장은 UTC, "오늘" 날짜 계산은 Asia/Seoul(KST) 기준으로 통일한다.
 */
public final class KstDateTime {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KstDateTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
