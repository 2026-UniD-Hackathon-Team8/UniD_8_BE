package com.unid8.damideo.daily.dto;

import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionType;
import java.time.LocalDate;
import java.util.List;

/**
 * 오늘의 데일리 조회 응답. 미제출 상태에서도 정답/해설은 절대 포함하지 않는다.
 */
public record DailyResponse(
        LocalDate dailyDate,
        boolean completed,
        int solvedCount,
        int totalCount,
        List<DailyQuestionItem> questions
) {

    public record DailyQuestionItem(
            Long questionId,
            int seq,
            QuestionType questionType,
            String questionTypeName,
            String prompt,
            String optionA,
            String optionB,
            boolean answered,
            AnswerPos selectedPos
    ) {

        public static DailyQuestionItem of(Question q,
                                           int seq,
                                           AnswerPos answerPos,
                                           AnswerPos selectedPos) {
            return new DailyQuestionItem(
                    q.getId(),
                    seq,
                    q.getQuestionType(),
                    q.getQuestionType().displayName(),
                    q.getPrompt(),
                    q.optionA(answerPos),
                    q.optionB(answerPos),
                    selectedPos != null,
                    selectedPos
            );
        }
    }
}
