package com.unid8.damideo.stats;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 약점 유형 판정.
 *
 * <ul>
 *   <li>계열마다 처음 푼 답 중 최근 6문제의 정답률을 본다</li>
 *   <li>계열마다 4문제 이상, 전체 20문제 이상일 때 유형을 연다</li>
 *   <li>정답률이 가장 낮은 계열이 유형이고, 모든 계열이 80% 이상이면 탐정형</li>
 *   <li>다른 계열이 지금 유형보다 10%p 이상 낮으면 후보로 두고, 다음 날에도 같은 후보면 바꾼다</li>
 * </ul>
 */
public final class WeaknessTypeJudge {

    public static final String DETECTIVE = "DETECTIVE";

    static final int SAMPLE_SIZE = 6;
    static final int MIN_PER_CATEGORY = 4;
    static final int MIN_TOTAL = 20;
    static final double DETECTIVE_ACCURACY = 0.8;
    static final double CHANGE_GAP = 0.1;
    private static final double EPSILON = 1e-9;

    private WeaknessTypeJudge() {
    }

    /**
     * @param categoryOrder 계열 순서 (근거 → 질문). 첫 판정의 동률은 이 순서로 정한다
     * @param firstAttempts 계열별 처음 푼 답의 정답 여부, 최신순
     * @param state         지금 저장된 유형 상태
     * @param today         오늘 날짜 (KST)
     */
    public static TypeJudgement judge(List<String> categoryOrder,
                                      Map<String, List<Boolean>> firstAttempts,
                                      TypeState state,
                                      LocalDate today) {
        Map<String, Double> accuracies = accuracies(categoryOrder, firstAttempts);
        if (!isOpen(categoryOrder, firstAttempts)) {
            return TypeJudgement.locked(state, accuracies);
        }

        String weakest = weakest(categoryOrder, accuracies);
        if (state.currentType() == null) {
            return TypeJudgement.changed(weakest, accuracies);
        }

        String candidate = candidate(state.currentType(), weakest, accuracies);
        if (candidate == null) {
            return TypeJudgement.kept(state.currentType(), null, null, accuracies);
        }
        if (candidate.equals(state.candidateType())
                && state.candidateSince() != null
                && state.candidateSince().isBefore(today)) {
            return TypeJudgement.changed(candidate, accuracies);
        }
        if (candidate.equals(state.candidateType()) && state.candidateSince() != null) {
            return TypeJudgement.kept(state.currentType(), candidate, state.candidateSince(), accuracies);
        }
        return TypeJudgement.kept(state.currentType(), candidate, today, accuracies);
    }

    static Map<String, Double> accuracies(List<String> categoryOrder, Map<String, List<Boolean>> firstAttempts) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (String category : categoryOrder) {
            List<Boolean> answers = firstAttempts.getOrDefault(category, List.of());
            List<Boolean> sample = answers.subList(0, Math.min(SAMPLE_SIZE, answers.size()));
            if (sample.isEmpty()) {
                continue;
            }
            long correct = sample.stream().filter(Boolean.TRUE::equals).count();
            result.put(category, (double) correct / sample.size());
        }
        return result;
    }

    static boolean isOpen(List<String> categoryOrder, Map<String, List<Boolean>> firstAttempts) {
        int total = 0;
        for (String category : categoryOrder) {
            int count = firstAttempts.getOrDefault(category, List.of()).size();
            if (count < MIN_PER_CATEGORY) {
                return false;
            }
            total += count;
        }
        return total >= MIN_TOTAL;
    }

    private static String weakest(List<String> categoryOrder, Map<String, Double> accuracies) {
        String weakest = null;
        double lowest = Double.MAX_VALUE;
        for (String category : categoryOrder) {
            double accuracy = accuracies.get(category);
            if (accuracy < lowest - EPSILON) {
                lowest = accuracy;
                weakest = category;
            }
        }
        return lowest >= DETECTIVE_ACCURACY - EPSILON ? DETECTIVE : weakest;
    }

    /**
     * 바꿀 만한 후보를 고른다. 없으면 null.
     */
    private static String candidate(String current, String weakest, Map<String, Double> accuracies) {
        if (Objects.equals(current, weakest)) {
            return null;
        }
        if (DETECTIVE.equals(current) || DETECTIVE.equals(weakest)) {
            return weakest;
        }
        Double currentAccuracy = accuracies.get(current);
        if (currentAccuracy == null) {
            return weakest;
        }
        double gap = currentAccuracy - accuracies.get(weakest);
        return gap >= CHANGE_GAP - EPSILON ? weakest : null;
    }

    public record TypeState(String currentType, String candidateType, LocalDate candidateSince) {

        public static TypeState empty() {
            return new TypeState(null, null, null);
        }
    }

    /**
     * @param opened  유형이 열렸는지 (조건 미달이면 false)
     * @param changed 유형이 새로 정해지거나 바뀌었는지. true면 유형 변경 이력을 남긴다
     */
    public record TypeJudgement(String currentType,
                                String candidateType,
                                LocalDate candidateSince,
                                boolean opened,
                                boolean changed,
                                Map<String, Double> accuracies) {

        static TypeJudgement locked(TypeState state, Map<String, Double> accuracies) {
            return new TypeJudgement(state.currentType(), state.candidateType(), state.candidateSince(),
                    false, false, accuracies);
        }

        static TypeJudgement changed(String type, Map<String, Double> accuracies) {
            return new TypeJudgement(type, null, null, true, true, accuracies);
        }

        static TypeJudgement kept(String type, String candidate, LocalDate since, Map<String, Double> accuracies) {
            return new TypeJudgement(type, candidate, since, true, false, accuracies);
        }
    }
}
