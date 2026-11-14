package com.unid8.damideo.question;

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
}
