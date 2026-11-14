package com.unid8.damideo.game.dto;

import com.unid8.damideo.game.GameMode;
import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.question.QuestionTacticNames;
import java.util.List;

/**
 * 판 결과 응답. 점수와 계열별 오답 수를 포함하며 유형 이름은 붙이지 않는다(GM_04).
 */
public record GameResultResponse(
        Long gameSessionId,
        GameMode gameMode,
        int totalCount,
        int correctCount,
        List<CategoryWrongCount> categoryWrongCounts,
        List<GameResultItem> questions
) {

    public record CategoryWrongCount(
            QuestionCategory category,
            String categoryName,
            int wrongCount
    ) {
    }

    public record GameResultItem(
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

        public static GameResultItem of(Question q,
                                        int seq,
                                        AnswerPos selectedPos,
                                        AnswerPos correctPos) {
            return new GameResultItem(
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
