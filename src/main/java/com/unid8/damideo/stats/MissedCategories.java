package com.unid8.damideo.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 판 결과 화면의 "많이 틀린 계열".
 */
public final class MissedCategories {

    private MissedCategories() {
    }

    /**
     * 가장 많이 틀린 계열을 계열 순서대로 돌려준다. 틀린 문제가 없으면 빈 목록.
     */
    public static List<String> mostMissed(List<String> categoryOrder, List<CategoryAnswer> answers) {
        Map<String, Integer> wrongCounts = new LinkedHashMap<>();
        for (String category : categoryOrder) {
            wrongCounts.put(category, 0);
        }
        for (CategoryAnswer answer : answers) {
            if (!answer.correct()) {
                wrongCounts.merge(answer.category(), 1, Integer::sum);
            }
        }

        int max = wrongCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> result = new ArrayList<>();
        if (max == 0) {
            return result;
        }
        wrongCounts.forEach((category, count) -> {
            if (count == max) {
                result.add(category);
            }
        });
        return result;
    }

    public record CategoryAnswer(String category, boolean correct) {
    }
}
