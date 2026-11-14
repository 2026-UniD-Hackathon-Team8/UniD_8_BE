package com.unid8.damideo.question;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * answer 도메인 연동 서비스가 등록되기 전까지 answers 테이블을 읽기 전용으로 조회하는 기본 어댑터.
 */
@Component
public class JdbcUserAnswerStatsAdapter implements UserAnswerStatsPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAnswerStatsAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserQuestionHistory findHistory(Long userId) {
        Set<Long> wrongIds = new HashSet<>();
        Set<Long> correctIds = new HashSet<>();

        jdbcTemplate.query(
                """
                SELECT question_id, MAX(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS ever_correct
                FROM answers
                WHERE user_id = ?
                GROUP BY question_id
                """,
                rs -> {
                    long qId = rs.getLong("question_id");
                    if (rs.getInt("ever_correct") == 1) {
                        correctIds.add(qId);
                    } else {
                        wrongIds.add(qId);
                    }
                },
                userId
        );

        return new UserQuestionHistory(wrongIds, correctIds);
    }

    @Override
    public Map<QuestionCategory, Double> findCategoryAccuracies(Long userId) {
        Map<QuestionCategory, Double> result = new EnumMap<>(QuestionCategory.class);
        jdbcTemplate.query(
                """
                SELECT q.category,
                       SUM(CASE WHEN a.is_correct = 1 THEN 1 ELSE 0 END) AS correct_cnt,
                       COUNT(*) AS total_cnt
                FROM answers a
                JOIN questions q ON q.id = a.question_id
                WHERE a.user_id = ? AND a.first_attempt = 1
                GROUP BY q.category
                """,
                rs -> {
                    String catStr = rs.getString("category");
                    long total = rs.getLong("total_cnt");
                    long correct = rs.getLong("correct_cnt");
                    if (total > 0) {
                        try {
                            result.put(QuestionCategory.valueOf(catStr), (double) correct / total);
                        } catch (IllegalArgumentException ignored) {
                            // 알 수 없는 카테고리 코드는 건너뛴다.
                        }
                    }
                },
                userId
        );
        return result;
    }
}
