package com.unid8.damideo.question;

import java.util.Arrays;

public enum QuestionCategory {
    EVIDENCE("근거"),
    CONTEXT("맥락"),
    LOGIC("논리"),
    TIMELINE("시점"),
    QUESTION("질문");

    private final String displayName;

    QuestionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * CSV 한글 계열명 또는 Enum 이름으로 {@link QuestionCategory}를 찾는다.
     */
    public static QuestionCategory fromCsvValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("계열 값이 비어 있습니다.");
        }
        String trimmed = raw.trim();
        return Arrays.stream(values())
                .filter(category -> category.displayName.equals(trimmed) || category.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 문제 계열입니다: " + raw));
    }
}
