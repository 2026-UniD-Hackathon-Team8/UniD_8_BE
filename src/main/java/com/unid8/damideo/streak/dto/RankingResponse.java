package com.unid8.damideo.streak.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RankingResponse(List<RankItem> top, MyRank me) {

    public record RankItem(int rank, String nickname, int bestWinStreak, OffsetDateTime achievedAt) {
    }

    /**
     * @param rank 기록이 없으면 null
     */
    public record MyRank(Integer rank, String nickname, int bestWinStreak) {
    }
}
