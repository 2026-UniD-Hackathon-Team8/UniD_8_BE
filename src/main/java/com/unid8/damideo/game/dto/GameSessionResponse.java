package com.unid8.damideo.game.dto;

import com.unid8.damideo.game.GameMode;
import com.unid8.damideo.game.GameStatus;
import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionType;
import java.util.List;

/**
 * 판 시작 / 이어 하기 응답. 정답·해설은 포함하지 않는다.
 */
public record GameSessionResponse(
        Long gameSessionId,
        GameMode gameMode,
        GameStatus status,
        int solvedCount,
        int totalCount,
        int nextSeq,
        List<GameQuestionItem> questions
) {

    public record GameQuestionItem(
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

        public static GameQuestionItem of(Question q,
                                          int seq,
                                          AnswerPos answerPos,
                                          AnswerPos selectedPos) {
            return new GameQuestionItem(
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
