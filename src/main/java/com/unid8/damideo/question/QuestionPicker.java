package com.unid8.damideo.question;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.daily.DailyErrorCode;
import com.unid8.damideo.daily.DailySetQuestionRepository;
import com.unid8.damideo.game.GameMode;
import com.unid8.damideo.question.UserAnswerStatsPort.UserQuestionHistory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 데일리, 실전 판, 연승 챌린지용 문제 선정기.
 * 연승 챌린지 서비스에서도 공용으로 호출한다.
 */
@Service
public class QuestionPicker {

    private static final int DAILY_EXCLUSION_DAYS = 14;

    private final QuestionRepository questionRepository;
    private final DailySetQuestionRepository dailySetQuestionRepository;
    private final UserAnswerStatsPort userAnswerStatsPort;

    public QuestionPicker(QuestionRepository questionRepository,
                          DailySetQuestionRepository dailySetQuestionRepository,
                          UserAnswerStatsPort userAnswerStatsPort) {
        this.questionRepository = questionRepository;
        this.dailySetQuestionRepository = dailySetQuestionRepository;
        this.userAnswerStatsPort = userAnswerStatsPort;
    }

    /**
     * 오늘(dailyDate)의 데일리 5문제를 계열 고정 순서(EVIDENCE → CONTEXT → LOGIC → TIMELINE → QUESTION)로 선정한다.
     */
    public List<Question> pickForDaily(LocalDate dailyDate) {
        List<Question> allActive = questionRepository.findAllByActiveTrue();
        Set<Long> recentIds = new HashSet<>(
                dailySetQuestionRepository.findQuestionIdsBetweenDates(
                        dailyDate.minusDays(DAILY_EXCLUSION_DAYS), dailyDate)
        );
        Random random = new Random(dailyDate.toEpochDay());

        List<Question> picked = new ArrayList<>(QuestionCategory.values().length);
        for (QuestionCategory category : QuestionCategory.values()) {
            List<Question> candidates = findDailyCandidates(allActive, category, recentIds);
            if (candidates.isEmpty()) {
                throw new BusinessException(DailyErrorCode.INSUFFICIENT_DAILY_QUESTIONS);
            }
            picked.add(candidates.get(random.nextInt(candidates.size())));
        }
        return picked;
    }

    /**
     * 일반 판 / 약점 재도전 판 문제를 선정한다.
     * 오늘 데일리 문제는 제외하고, 안 푼 문제 → 틀렸던 문제 → 맞혔던 문제 순으로 고른 뒤
     * 난이도 오름차순 정렬 및 같은 수법 연속 배치를 최소화한다.
     */
    public List<Question> pickForGame(Long userId, GameMode mode, int requestedCount, LocalDate today) {
        Set<Long> todayDailyIds = new HashSet<>(dailySetQuestionRepository.findQuestionIdsByDailyDate(today));
        List<Question> pool = questionRepository.findAllByActiveTrue().stream()
                .filter(q -> !todayDailyIds.contains(q.getId()))
                .toList();
        if (pool.isEmpty()) {
            return List.of();
        }

        int targetCount = Math.min(requestedCount, pool.size());
        UserQuestionHistory history = userAnswerStatsPort.findHistory(userId);
        Random random = new Random();

        Map<QuestionCategory, List<Question>> byCategory = groupPrioritizedByCategory(pool, history, random);
        Map<QuestionCategory, Integer> quotas = allocateQuotas(userId, mode, targetCount, random);

        List<Question> selected = new ArrayList<>(targetCount);
        Set<Long> selectedIds = new HashSet<>();

        for (QuestionCategory category : QuestionCategory.values()) {
            int quota = quotas.getOrDefault(category, 0);
            List<Question> catList = byCategory.getOrDefault(category, List.of());
            int taken = 0;
            for (Question q : catList) {
                if (taken >= quota) {
                    break;
                }
                if (selectedIds.add(q.getId())) {
                    selected.add(q);
                    taken++;
                }
            }
        }

        if (selected.size() < targetCount) {
            List<Question> remainder = prioritizePool(pool, history, random);
            for (Question q : remainder) {
                if (selected.size() >= targetCount) {
                    break;
                }
                if (selectedIds.add(q.getId())) {
                    selected.add(q);
                }
            }
        }

        sortByDifficultyAndAvoidConsecutiveTactic(selected);
        return selected;
    }

    /**
     * 연승 챌린지 다음 문제를 고른다.
     * 오늘 데일리 문제와 이번 도전에서 이미 낸 문제(excludedQuestionIds)를 제외하고,
     * 안 푼 문제 → 틀렸던 문제 → 맞혔던 문제 순으로 1문제를 반환한다.
     */
    public Optional<Question> pickForStreak(Long userId, Set<Long> excludedQuestionIds) {
        LocalDate today = KstDateTime.today();
        Set<Long> excluded = new HashSet<>(dailySetQuestionRepository.findQuestionIdsByDailyDate(today));
        if (excludedQuestionIds != null) {
            excluded.addAll(excludedQuestionIds);
        }

        List<Question> pool = questionRepository.findAllByActiveTrue().stream()
                .filter(q -> !excluded.contains(q.getId()))
                .toList();
        if (pool.isEmpty()) {
            return Optional.empty();
        }

        UserQuestionHistory history = userAnswerStatsPort.findHistory(userId);
        List<Question> prioritized = prioritizePool(pool, history, new Random());
        return prioritized.isEmpty() ? Optional.empty() : Optional.of(prioritized.get(0));
    }

    private List<Question> findDailyCandidates(List<Question> allActive,
                                               QuestionCategory category,
                                               Set<Long> recentIds) {
        List<Question> tier1 = allActive.stream()
                .filter(q -> q.getCategory() == category && q.isDailyEligible() && !recentIds.contains(q.getId()))
                .toList();
        if (!tier1.isEmpty()) {
            return tier1;
        }

        List<Question> tier2 = allActive.stream()
                .filter(q -> q.getCategory() == category && q.isDailyEligible())
                .toList();
        if (!tier2.isEmpty()) {
            return tier2;
        }

        return allActive.stream()
                .filter(q -> q.getCategory() == category)
                .toList();
    }

    private Map<QuestionCategory, List<Question>> groupPrioritizedByCategory(List<Question> pool,
                                                                             UserQuestionHistory history,
                                                                             Random random) {
        Map<QuestionCategory, List<Question>> raw = new EnumMap<>(QuestionCategory.class);
        for (QuestionCategory category : QuestionCategory.values()) {
            raw.put(category, new ArrayList<>());
        }
        for (Question q : pool) {
            raw.get(q.getCategory()).add(q);
        }
        Map<QuestionCategory, List<Question>> result = new EnumMap<>(QuestionCategory.class);
        for (QuestionCategory category : QuestionCategory.values()) {
            result.put(category, prioritizePool(raw.get(category), history, random));
        }
        return result;
    }

    private List<Question> prioritizePool(List<Question> pool, UserQuestionHistory history, Random random) {
        List<Question> unsolved = new ArrayList<>();
        List<Question> wrong = new ArrayList<>();
        List<Question> correct = new ArrayList<>();

        for (Question q : pool) {
            if (!history.isSolved(q.getId())) {
                unsolved.add(q);
            } else if (history.wrongQuestionIds().contains(q.getId())) {
                wrong.add(q);
            } else {
                correct.add(q);
            }
        }

        Collections.shuffle(unsolved, random);
        Collections.shuffle(wrong, random);
        Collections.shuffle(correct, random);

        List<Question> merged = new ArrayList<>(pool.size());
        merged.addAll(unsolved);
        merged.addAll(wrong);
        merged.addAll(correct);
        return merged;
    }

    private Map<QuestionCategory, Integer> allocateQuotas(Long userId,
                                                          GameMode mode,
                                                          int targetCount,
                                                          Random random) {
        Map<QuestionCategory, Integer> quotas = new EnumMap<>(QuestionCategory.class);
        QuestionCategory[] categories = QuestionCategory.values();

        if (mode == GameMode.WEAKNESS_RETRY) {
            Map<QuestionCategory, Double> accuracies = userAnswerStatsPort.findCategoryAccuracies(userId);
            if (accuracies.size() >= 2) {
                return allocateWeaknessQuotas(accuracies, targetCount);
            }
        }

        int base = targetCount / categories.length;
        int rem = targetCount % categories.length;
        for (QuestionCategory c : categories) {
            quotas.put(c, base);
        }

        List<QuestionCategory> shuffled = new ArrayList<>(Arrays.asList(categories));
        Collections.shuffle(shuffled, random);
        for (int i = 0; i < rem; i++) {
            QuestionCategory c = shuffled.get(i);
            quotas.put(c, quotas.get(c) + 1);
        }
        return quotas;
    }

    private Map<QuestionCategory, Integer> allocateWeaknessQuotas(Map<QuestionCategory, Double> accuracies,
                                                                  int targetCount) {
        List<QuestionCategory> ordered = new ArrayList<>(Arrays.asList(QuestionCategory.values()));
        ordered.sort(Comparator.comparingDouble(c -> accuracies.getOrDefault(c, 1.0)));

        // 약한 계열(정답률 낮은 순)에 가중치 부여: 상위 2개 약점 계열에 비중을 높인다.
        int[] weights = {4, 3, 1, 1, 1};
        int totalWeight = Arrays.stream(weights).sum();

        Map<QuestionCategory, Integer> quotas = new EnumMap<>(QuestionCategory.class);
        int assigned = 0;
        for (int i = 0; i < ordered.size(); i++) {
            int q = (targetCount * weights[i]) / totalWeight;
            quotas.put(ordered.get(i), q);
            assigned += q;
        }
        for (int i = 0; assigned < targetCount; i = (i + 1) % ordered.size()) {
            QuestionCategory c = ordered.get(i);
            quotas.put(c, quotas.get(c) + 1);
            assigned++;
        }
        return quotas;
    }

    private void sortByDifficultyAndAvoidConsecutiveTactic(List<Question> questions) {
        questions.sort(Comparator.comparingInt(Question::getDifficulty));
        for (int i = 1; i < questions.size(); i++) {
            Question prev = questions.get(i - 1);
            Question curr = questions.get(i);
            if (!prev.getTactic().equals(curr.getTactic())) {
                continue;
            }
            for (int j = i + 1; j < questions.size(); j++) {
                Question candidate = questions.get(j);
                if (candidate.getDifficulty() != curr.getDifficulty()) {
                    break;
                }
                if (!candidate.getTactic().equals(prev.getTactic())) {
                    Collections.swap(questions, i, j);
                    break;
                }
            }
        }
    }
}
