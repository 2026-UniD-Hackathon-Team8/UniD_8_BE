package com.unid8.damideo.streak;

import com.unid8.damideo.question.AnswerPos;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 연승 챌린지 답을 answers 테이블에 source = STREAK로 저장한다.
 */
@Component
@RequiredArgsConstructor
public class StreakAnswerWriter {

    private final JdbcTemplate jdbcTemplate;

    public void record(Long userId, Long challengeId, Long questionId, AnswerPos selectedPos, boolean correct) {
        Integer solvedBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM answers WHERE user_id = ? AND question_id = ?",
                Integer.class, userId, questionId);
        jdbcTemplate.update(
                """
                INSERT INTO answers
                    (user_id, question_id, source, streak_challenge_id, selected_pos, is_correct, first_attempt)
                VALUES (?, ?, 'STREAK', ?, ?, ?, ?)
                """,
                userId, questionId, challengeId, selectedPos.name(), correct,
                solvedBefore == null || solvedBefore == 0);
    }

    public Set<Long> askedQuestionIds(Long challengeId) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT question_id FROM answers WHERE streak_challenge_id = ?", Long.class, challengeId));
    }
}
