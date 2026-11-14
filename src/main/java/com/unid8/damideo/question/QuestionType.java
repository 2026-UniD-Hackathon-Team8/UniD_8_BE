package com.unid8.damideo.question;

import java.util.Arrays;

public enum QuestionType {
    CHOICE_ANSWER("답 고르기"),
    CHOICE_QUESTION("질문 고르기"),
    CHOICE_DIALOGUE("대화 고르기");

    private final String displayName;

    QuestionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * CSV 한글 유형명 또는 Enum 이름으로 {@link QuestionType}을 찾는다.
     */
    public static QuestionType fromCsvValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("문제 유형 값이 비어 있습니다.");
        }
        String trimmed = raw.trim();
        return Arrays.stream(values())
                .filter(type -> type.displayName.equals(trimmed) || type.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 문제 유형입니다: " + raw));
    }
}
