package com.unid8.damideo.question;

public enum AnswerPos {
    A,
    B;

    public AnswerPos opposite() {
        return this == A ? B : A;
    }
}
