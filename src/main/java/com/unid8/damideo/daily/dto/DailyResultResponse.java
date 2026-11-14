package com.unid8.damideo.daily.dto;

import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.question.QuestionTacticNames;
import java.time.LocalDate;
import java.util.List;

public record DailyResultResponse(
        LocalDate dailyDate,
        int totalCount,
        int correctCount,
        List<DailyResultItem> questions
) {

    public record DailyResultItem(
            Long questionId,
            int seq,
            QuestionCategory category,
            String categoryName,
            String tactic,
            String tacticName,
            AnswerPos selectedPos,
            AnswerPos correctPos,
            boolean correct,
            String explanation,
            String checkQuestion,
            String source
    ) {

        public static DailyResultItem of(Question q,
                                         int seq,
                                         AnswerPos selectedPos,
                                         AnswerPos correctPos) {
            return new DailyResultItem(
                    q.getId(),
                    seq,
                    q.getCategory(),
                    q.getCategory().displayName(),
                    q.getTactic(),
                    QuestionTacticNames.resolve(q.getTactic()),
                    selectedPos,
                    correctPos,
                    selectedPos == correctPos,
                    q.getExplanation(),
                    q.getCheckQuestion(),
                    q.getSource()
            );
        }
    }
}
