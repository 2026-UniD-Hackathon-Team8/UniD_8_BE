package com.unid8.damideo.question.dto;

import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.question.QuestionTacticNames;

/**
 * 데일리·실전 판 답 제출 시 반환하는 공통 정답 공개 응답.
 */
public record AnswerRevealResponse(
        Long questionId,
        AnswerPos selectedPos,
        AnswerPos correctPos,
        boolean correct,
        QuestionCategory category,
        String categoryName,
        String tactic,
        String tacticName,
        String explanation,
        String checkQuestion,
        String source,
        String credibleAiAnswer,
        String targetAiAnswer,
        boolean completed
) {

    public static AnswerRevealResponse of(Question question,
                                          AnswerPos selectedPos,
                                          AnswerPos correctPos,
                                          boolean completed) {
        return new AnswerRevealResponse(
                question.getId(),
                selectedPos,
                correctPos,
                selectedPos == correctPos,
                question.getCategory(),
                question.getCategory().displayName(),
                question.getTactic(),
                QuestionTacticNames.resolve(question.getTactic()),
                question.getExplanation(),
                question.getCheckQuestion(),
                question.getSource(),
                question.getCredibleAiAnswer(),
                question.getTargetAiAnswer(),
                completed
        );
    }
}
