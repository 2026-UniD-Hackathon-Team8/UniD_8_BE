package com.unid8.damideo.user.dto;

import com.unid8.damideo.question.QuestionCategory;
import java.time.LocalDate;
import java.util.List;

/**
 * @param available         3회 미만이면 false
 * @param remainingSessions 성장 기록이 열리기까지 남은 회 수
 */
public record GrowthResponse(boolean available,
                             int remainingSessions,
                             List<Score> scoreTrend,
                             List<CategoryAccuracy> categoryAccuracy,
                             List<TypeRecord> typeHistory) {

    /**
     * @param sessionType GAME / DAILY
     */
    public record Score(LocalDate date, String sessionType, int score, int total) {
    }

    /**
     * @param early  처음 3회의 정답률(0~1). 그 계열 문제가 없었으면 null
     * @param recent 최근 3회의 정답률(0~1). 그 계열 문제가 없었으면 null
     */
    public record CategoryAccuracy(QuestionCategory category, String categoryName, Double early, Double recent) {
    }

    public record TypeRecord(String code, String name, LocalDate recordedDate) {
    }
}
