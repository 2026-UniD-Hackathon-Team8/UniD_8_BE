package com.unid8.damideo.question;

import com.unid8.damideo.answer.FirstAttemptReader;
import com.unid8.damideo.answer.FirstAttemptReader.FirstAttempts;
import com.unid8.damideo.stats.WeaknessTypeJudge;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * answer 도메인 연동 서비스가 등록되기 전까지 answers 테이블을 읽기 전용으로 조회하는 기본 어댑터.
 */
@Component
public class JdbcUserAnswerStatsAdapter implements UserAnswerStatsPort {

    private static final List<String> CATEGORY_ORDER =
            Arrays.stream(QuestionCategory.values()).map(Enum::name).toList();

    private final JdbcTemplate jdbcTemplate;
    private final FirstAttemptReader firstAttemptReader;

    public JdbcUserAnswerStatsAdapter(JdbcTemplate jdbcTemplate, FirstAttemptReader firstAttemptReader) {
        this.jdbcTemplate = jdbcTemplate;
        this.firstAttemptReader = firstAttemptReader;
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

    /**
     * 계열별로 처음 푼 답 중 최근 6문제의 정답률. 유형 판정과 같은 기준이다.
     */
    @Override
    public Map<QuestionCategory, Double> findCategoryAccuracies(Long userId) {
        FirstAttempts attempts = firstAttemptReader.read(userId);
        Map<String, Double> accuracies = WeaknessTypeJudge.accuracies(CATEGORY_ORDER, attempts.byCategory());
        Map<QuestionCategory, Double> result = new EnumMap<>(QuestionCategory.class);
        accuracies.forEach((category, accuracy) -> result.put(QuestionCategory.valueOf(category), accuracy));
        return result;
    }
}
