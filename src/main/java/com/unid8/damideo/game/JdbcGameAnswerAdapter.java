package com.unid8.damideo.game;

import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.user.UserRecordService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * answers 테이블을 통해 실전 판 답안을 멱등 저장·조회하는 기본 어댑터.
 */
@Component
public class JdbcGameAnswerAdapter implements GameAnswerPort {

    private final JdbcTemplate jdbcTemplate;
    private final UserRecordService userRecordService;

    public JdbcGameAnswerAdapter(JdbcTemplate jdbcTemplate, UserRecordService userRecordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRecordService = userRecordService;
    }

    @Override
    public Map<Long, SubmittedGameAnswer> findAnswers(Long gameSessionId) {
        Map<Long, SubmittedGameAnswer> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT question_id, selected_pos, is_correct
                FROM answers
                WHERE source = 'GAME' AND game_session_id = ?
                """,
                rs -> {
                    long qId = rs.getLong("question_id");
                    AnswerPos pos = AnswerPos.valueOf(rs.getString("selected_pos"));
                    boolean correct = rs.getBoolean("is_correct");
                    result.put(qId, new SubmittedGameAnswer(qId, pos, correct));
                },
                gameSessionId
        );
        return result;
    }

    @Override
    public SubmittedGameAnswer recordAnswer(Long userId,
                                            Long gameSessionId,
                                            Long questionId,
                                            AnswerPos selectedPos,
                                            boolean correct) {
        SubmittedGameAnswer existing = findOne(gameSessionId, questionId);
        if (existing != null) {
            return existing;
        }

        boolean firstAttempt = isFirstAttempt(userId, questionId);
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO answers
                        (user_id, question_id, source, game_session_id, selected_pos, is_correct, first_attempt)
                    VALUES (?, ?, 'GAME', ?, ?, ?, ?)
                    """,
                    userId,
                    questionId,
                    gameSessionId,
                    selectedPos.name(),
                    correct,
                    firstAttempt
            );
            return new SubmittedGameAnswer(questionId, selectedPos, correct);
        } catch (DataIntegrityViolationException ex) {
            SubmittedGameAnswer concurrent = findOne(gameSessionId, questionId);
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
    }

    @Override
    public void onGameCompleted(Long userId, Long gameSessionId) {
        userRecordService.completeGame(userId, gameSessionId);
    }

    private SubmittedGameAnswer findOne(Long gameSessionId, Long questionId) {
        return jdbcTemplate.query(
                """
                SELECT question_id, selected_pos, is_correct
                FROM answers
                WHERE game_session_id = ? AND question_id = ?
                LIMIT 1
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new SubmittedGameAnswer(
                            rs.getLong("question_id"),
                            AnswerPos.valueOf(rs.getString("selected_pos")),
                            rs.getBoolean("is_correct")
                    );
                },
                gameSessionId,
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
