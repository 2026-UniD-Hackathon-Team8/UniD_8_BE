package com.unid8.damideo.question;

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
}
