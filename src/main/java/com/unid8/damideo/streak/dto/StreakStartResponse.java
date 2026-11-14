package com.unid8.damideo.streak.dto;

import com.unid8.damideo.game.dto.GameSessionResponse.GameQuestionItem;

public record StreakStartResponse(Long challengeId, int bestWinStreak, GameQuestionItem question) {
}
