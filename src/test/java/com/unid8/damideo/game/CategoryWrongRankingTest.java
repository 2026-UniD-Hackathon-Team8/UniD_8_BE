package com.unid8.damideo.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.question.QuestionCategory;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CategoryWrongRankingTest {

    // 오답 수가 많은 계열이 먼저다
    @Test
    void sort_moreWrongFirst() {
        Map<QuestionCategory, Integer> wrong = wrong(QuestionCategory.EVIDENCE, 1, QuestionCategory.LOGIC, 3);

        assertThat(order(wrong, Map.of())).containsExactly(QuestionCategory.LOGIC, QuestionCategory.EVIDENCE);
    }

    // 오답 수가 같으면 평소 정답률이 낮은 계열이 먼저다
    @Test
    void sort_tieBrokenByLowerAccuracy() {
        Map<QuestionCategory, Integer> wrong = wrong(QuestionCategory.LOGIC, 2, QuestionCategory.QUESTION, 2);
        Map<QuestionCategory, Double> accuracies = Map.of(QuestionCategory.LOGIC, 0.6, QuestionCategory.QUESTION, 0.2);

        assertThat(order(wrong, accuracies)).containsExactly(QuestionCategory.QUESTION, QuestionCategory.LOGIC);
    }

    // 정답률 기록이 없으면 계열 순서로 정한다
    @Test
    void sort_noAccuracyFallsBackToCategoryOrder() {
        Map<QuestionCategory, Integer> wrong = wrong(QuestionCategory.QUESTION, 2, QuestionCategory.CONTEXT, 2);

        assertThat(order(wrong, Map.of())).containsExactly(QuestionCategory.CONTEXT, QuestionCategory.QUESTION);
    }

    private static Map<QuestionCategory, Integer> wrong(QuestionCategory c1, int n1, QuestionCategory c2, int n2) {
        Map<QuestionCategory, Integer> wrong = new EnumMap<>(QuestionCategory.class);
        wrong.put(c1, n1);
        wrong.put(c2, n2);
        return wrong;
    }

    private static List<QuestionCategory> order(Map<QuestionCategory, Integer> wrong,
                                                Map<QuestionCategory, Double> accuracies) {
        return CategoryWrongRanking.sort(wrong, accuracies).stream().map(Map.Entry::getKey).toList();
    }
}
