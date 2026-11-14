package com.unid8.damideo.answer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 사용자가 처음 푼 답을 계열별로 읽는다. 유형 판정과 프로필에서 쓴다.
 */
@Component
@RequiredArgsConstructor
public class FirstAttemptReader {

    private final JdbcTemplate jdbcTemplate;

    public FirstAttempts read(Long userId) {
        Map<String, List<Boolean>> byCategory = new LinkedHashMap<>();
        int[] total = {0};
        jdbcTemplate.query(
                """
                SELECT q.category, a.is_correct
                FROM answers a
                JOIN questions q ON q.id = a.question_id
                WHERE a.user_id = ? AND a.first_attempt = TRUE
                ORDER BY a.submitted_at DESC, a.id DESC
                """,
                rs -> {
                    byCategory.computeIfAbsent(rs.getString("category"), key -> new ArrayList<>())
                            .add(rs.getBoolean("is_correct"));
                    total[0]++;
                },
                userId);
        return new FirstAttempts(byCategory, total[0]);
    }

    /**
     * @param byCategory 계열별 정답 여부, 최신순
     * @param total      처음 푼 답 전체 수
     */
    public record FirstAttempts(Map<String, List<Boolean>> byCategory, int total) {
    }
}
