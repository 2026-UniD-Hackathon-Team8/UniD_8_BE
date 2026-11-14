package com.unid8.damideo.user.dto;

import com.unid8.damideo.user.dto.ProfileResponse.Streak;
import java.time.LocalDate;

/**
 * @param inProgressGameId 끝내지 않은 판이 없으면 null
 */
public record HomeResponse(String nickname, Daily daily, Streak streak, Long inProgressGameId, int bestWinStreak) {

    /**
     * @param status NOT_STARTED / IN_PROGRESS / COMPLETED
     */
    public record Daily(LocalDate date, String status, int solvedCount, int total) {
    }
}
