package com.unid8.damideo.question;

import java.util.Map;
import java.util.Set;

/**
 * 문제 선정(판/연승)을 위해 사용자의 풀이 이력을 읽기 전용으로 조회하는 포트.
 */
public interface UserAnswerStatsPort {

    UserQuestionHistory findHistory(Long userId);

    /**
     * 약점 재도전(WEAKNESS_RETRY)용 계열별 정답률(0.0 ~ 1.0)을 반환한다.
     * 풀이 기록이 충분하지 않으면 빈 맵을 반환한다.
     */
    Map<QuestionCategory, Double> findCategoryAccuracies(Long userId);

    record UserQuestionHistory(Set<Long> wrongQuestionIds, Set<Long> correctQuestionIds) {

        public boolean isSolved(Long questionId) {
            return wrongQuestionIds.contains(questionId) || correctQuestionIds.contains(questionId);
        }
    }
}
