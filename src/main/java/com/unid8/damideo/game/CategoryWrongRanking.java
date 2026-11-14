package com.unid8.damideo.game;

import com.unid8.damideo.question.QuestionCategory;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 판 결과의 계열별 오답 순서.
 * 오답 수가 많은 순, 같으면 평소 정답률(계열별 최근 첫 시도)이 낮은 순, 그래도 같으면 계열 순서.
 */
final class CategoryWrongRanking {

    private CategoryWrongRanking() {
    }

    static List<Map.Entry<QuestionCategory, Integer>> sort(Map<QuestionCategory, Integer> wrongByCategory,
                                                         Map<QuestionCategory, Double> accuracies) {
        return wrongByCategory.entrySet().stream()
                .sorted(Map.Entry.<QuestionCategory, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> accuracies.getOrDefault(e.getKey(), 1.0))
                        .thenComparing(e -> e.getKey().ordinal()))
                .toList();
    }
}
