package com.unid8.damideo.streak.dto;

import com.unid8.damideo.game.dto.GameSessionResponse.GameQuestionItem;
import com.unid8.damideo.question.dto.AnswerRevealResponse;

/**
 * @param endReason WRONG(틀림) / COMPLETED(완주) / null(진행 중)
 */
public record StreakAnswerResponse(AnswerRevealResponse reveal,
                                   int currentWinStreak,
                                   int bestWinStreak,
                                   boolean newRecord,
                                   boolean ended,
                                   String endReason,
                                   GameQuestionItem nextQuestion) {
}
