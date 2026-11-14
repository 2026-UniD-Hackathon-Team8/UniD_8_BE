package com.unid8.damideo.stats;

import java.util.List;

/**
 * 나의 성장: 처음 몇 회와 최근 몇 회의 정답률 비교, 회별 점수 추이.
 * 한 회는 판 한 번이나 데일리 한 번이다. 약점 재도전 판은 빼고 계산한다.
 */
public final class GrowthCalculator {

    static final int COMPARE_ROUNDS = 3;

    private GrowthCalculator() {
    }

    /**
     * @param rounds 오래된 순으로 정렬된 회 목록 (처음 푼 답만 센 값)
     */
    public static Growth calculate(List<Round> rounds) {
        List<Round> counted = rounds.stream()
                .filter(round -> !round.weaknessRetry())
                .filter(round -> round.total() > 0)
                .toList();

        List<Integer> scores = counted.stream().map(GrowthCalculator::percent).toList();
        if (counted.size() < 2) {
            return new Growth(scores, null, null, null);
        }

        int size = Math.min(COMPARE_ROUNDS, counted.size() / 2);
        int first = percent(counted.subList(0, size));
        int recent = percent(counted.subList(counted.size() - size, counted.size()));
        return new Growth(scores, first, recent, recent - first);
    }

    private static int percent(Round round) {
        return (int) Math.round(round.correct() * 100.0 / round.total());
    }

    /**
     * 문제 단위로 합쳐서 계산한다 (판 10문제와 데일리 5문제의 길이가 달라서).
     */
    private static int percent(List<Round> rounds) {
        int correct = rounds.stream().mapToInt(Round::correct).sum();
        int total = rounds.stream().mapToInt(Round::total).sum();
        return (int) Math.round(correct * 100.0 / total);
    }

    public record Round(int correct, int total, boolean weaknessRetry) {
    }

    /**
     * @param scores       회별 정답률(%) 추이
     * @param firstScore   처음 회들의 정답률(%). 2회 미만이면 null
     * @param recentScore  최근 회들의 정답률(%). 2회 미만이면 null
     * @param change       최근 - 처음 (%p). 2회 미만이면 null
     */
    public record Growth(List<Integer> scores, Integer firstScore, Integer recentScore, Integer change) {
    }
}
