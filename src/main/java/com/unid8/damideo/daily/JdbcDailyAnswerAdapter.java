package com.unid8.damideo.daily;

import com.unid8.damideo.question.AnswerPos;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * answers 테이블을 통해 데일리 답안을 멱등 저장·조회하는 기본 어댑터.
 */
@Component
public class JdbcDailyAnswerAdapter implements DailyAnswerPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDailyAnswerAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<Long, SubmittedDailyAnswer> findAnswers(Long userId, LocalDate dailyDate) {
        Map<Long, SubmittedDailyAnswer> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT question_id, selected_pos, is_correct
                FROM answers
                WHERE user_id = ? AND source = 'DAILY' AND daily_date = ?
                """,
                rs -> {
                    long qId = rs.getLong("question_id");
                    AnswerPos pos = AnswerPos.valueOf(rs.getString("selected_pos"));
                    boolean correct = rs.getBoolean("is_correct");
                    result.put(qId, new SubmittedDailyAnswer(qId, pos, correct));
                },
                userId,
                Date.valueOf(dailyDate)
        );
        return result;
    }

    @Override
    public SubmittedDailyAnswer recordAnswer(Long userId,
                                             Long questionId,
                                             LocalDate dailyDate,
                                             AnswerPos selectedPos,
                                             boolean correct) {
        SubmittedDailyAnswer existing = findOne(userId, questionId, dailyDate);
        if (existing != null) {
            return existing;
        }

        boolean firstAttempt = isFirstAttempt(userId, questionId);
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO answers
                        (user_id, question_id, source, daily_date, selected_pos, is_correct, first_attempt)
                    VALUES (?, ?, 'DAILY', ?, ?, ?, ?)
                    """,
                    userId,
                    questionId,
                    Date.valueOf(dailyDate),
                    selectedPos.name(),
                    correct,
                    firstAttempt
            );
            return new SubmittedDailyAnswer(questionId, selectedPos, correct);
        } catch (DataIntegrityViolationException ex) {
            SubmittedDailyAnswer concurrent = findOne(userId, questionId, dailyDate);
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
    }

    @Override
    public void onDailyCompleted(Long userId, LocalDate dailyDate) {
        // 팀원 담당 UserRecordService.completeDaily 연동 지점
    }

    private SubmittedDailyAnswer findOne(Long userId, Long questionId, LocalDate dailyDate) {
        return jdbcTemplate.query(
                """
                SELECT question_id, selected_pos, is_correct
                FROM answers
                WHERE user_id = ? AND daily_date = ? AND question_id = ?
                LIMIT 1
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new SubmittedDailyAnswer(
                            rs.getLong("question_id"),
                            AnswerPos.valueOf(rs.getString("selected_pos")),
                            rs.getBoolean("is_correct")
                    );
                },
                userId,
                Date.valueOf(dailyDate),
                questionId
        );
    }

    private boolean isFirstAttempt(Long userId, Long questionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM answers WHERE user_id = ? AND question_id = ?",
                Integer.class,
                userId,
                questionId
        );
        return count == null || count == 0;
    }
}
