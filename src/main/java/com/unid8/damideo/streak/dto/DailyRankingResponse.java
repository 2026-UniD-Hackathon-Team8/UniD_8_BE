package com.unid8.damideo.streak.dto;

import java.util.List;

public record DailyRankingResponse(List<RankItem> top, MyRank me) {

    public record RankItem(int rank, String nickname, int currentStreak, int longestStreak) {
    }

    /**
     * @param rank 현재 연속 기록이 없으면 null
     */
    public record MyRank(Integer rank, String nickname, int currentStreak, int longestStreak) {
    }
}
