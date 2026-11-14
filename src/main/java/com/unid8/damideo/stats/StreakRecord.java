package com.unid8.damideo.stats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 연승 챌린지 최고 기록과 연승 랭킹.
 */
public final class StreakRecord {

    private StreakRecord() {
    }

    /**
     * 도전이 끝났을 때 최고 기록을 갱신한다. 같거나 낮으면 기존 기록(먼저 달성한 시각)을 유지한다.
     */
    public static Best updateBest(Best current, int finalWins, LocalDateTime endedAt) {
        if (finalWins > current.wins()) {
            return new Best(finalWins, endedAt);
        }
        return current;
    }

    /**
     * 최고 연승 내림차순, 같으면 먼저 달성한 순으로 정렬하고 순위를 매긴다.
     * 연승 수가 같으면 같은 순위다 (1, 1, 3).
     */
    public static <T> List<Ranked<T>> rank(List<Entry<T>> entries) {
        List<Entry<T>> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.<Entry<T>>comparingInt(e -> e.best().wins()).reversed()
                .thenComparing(e -> e.best().achievedAt(), Comparator.nullsLast(Comparator.naturalOrder())));

        List<Ranked<T>> result = new ArrayList<>();
        int rank = 0;
        int previousWins = -1;
        for (int i = 0; i < sorted.size(); i++) {
            Entry<T> entry = sorted.get(i);
            if (entry.best().wins() != previousWins) {
                rank = i + 1;
                previousWins = entry.best().wins();
            }
            result.add(new Ranked<>(rank, entry.owner(), entry.best()));
        }
        return result;
    }

    public record Best(int wins, LocalDateTime achievedAt) {

        public static Best none() {
            return new Best(0, null);
        }
    }

    public record Entry<T>(T owner, Best best) {
    }

    public record Ranked<T>(int rank, T owner, Best best) {
    }
}
