package com.unid8.damideo.answer.dto;

import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.question.QuestionType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @param memoCount 메모를 남긴 문제 수
 */
public record WrongAnswersResponse(List<WrongAnswerItem> items, int memoCount) {

    /**
     * 틀린 답이므로 내가 고른 쪽은 믿을 쪽, 정답은 골라낼 쪽이다.
     */
    public record WrongAnswerItem(Long questionId,
                                  QuestionType questionType,
                                  String questionTypeName,
                                  String prompt,
                                  String myChoiceText,
                                  String answerChoiceText,
                                  QuestionCategory category,
                                  String categoryName,
                                  String tactic,
                                  String tacticName,
                                  String explanation,
                                  String checkQuestion,
                                  String source,
                                  String credibleAiAnswer,
                                  String targetAiAnswer,
                                  OffsetDateTime answeredAt,
                                  String memo) {
    }
}
