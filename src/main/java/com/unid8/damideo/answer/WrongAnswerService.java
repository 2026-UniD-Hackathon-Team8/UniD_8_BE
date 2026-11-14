package com.unid8.damideo.answer;

import com.unid8.damideo.answer.dto.MemoResponse;
import com.unid8.damideo.answer.dto.WrongAnswersResponse;
import com.unid8.damideo.answer.dto.WrongAnswersResponse.WrongAnswerItem;
import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.question.QuestionTacticNames;
import com.unid8.damideo.question.QuestionType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오답노트. 따로 테이블을 두지 않고 answers에서 틀린 답을 읽는다. 메모만 wrong_answer_notes에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class WrongAnswerService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 틀린 답을 최신순으로. 같은 문제를 여러 번 틀렸으면 최신 1건만.
     */
    @Transactional(readOnly = true)
    public WrongAnswersResponse getWrongAnswers(Long userId, QuestionCategory category) {
        List<Object> params = new ArrayList<>(List.of(userId));
        String categoryFilter = "";
        if (category != null) {
            categoryFilter = " AND q.category = ?";
            params.add(category.name());
        }

        List<WrongAnswerItem> items = jdbcTemplate.query(
                """
                SELECT q.id, q.question_type, q.prompt, q.credible_content, q.target_content,
                       q.category, q.tactic, q.explanation, q.check_question, q.source,
                       q.credible_ai_answer, q.target_ai_answer, a.submitted_at, n.memo
                FROM answers a
                JOIN questions q ON q.id = a.question_id
                LEFT JOIN wrong_answer_notes n ON n.user_id = a.user_id AND n.question_id = a.question_id
                WHERE a.user_id = ? AND a.is_correct = FALSE
                  AND a.id = (SELECT MAX(a2.id) FROM answers a2
                              WHERE a2.user_id = a.user_id AND a2.question_id = a.question_id
                                AND a2.is_correct = FALSE)
                """ + categoryFilter + """

                ORDER BY a.submitted_at DESC, a.id DESC
                """,
                (rs, rowNum) -> {
                    QuestionType type = QuestionType.valueOf(rs.getString("question_type"));
                    QuestionCategory questionCategory = QuestionCategory.valueOf(rs.getString("category"));
                    String tactic = rs.getString("tactic");
                    return new WrongAnswerItem(
                            rs.getLong("id"),
                            type,
                            type.displayName(),
                            rs.getString("prompt"),
                            rs.getString("credible_content"),
                            rs.getString("target_content"),
                            questionCategory,
                            questionCategory.displayName(),
                            tactic,
                            QuestionTacticNames.resolve(tactic),
                            rs.getString("explanation"),
                            rs.getString("check_question"),
                            rs.getString("source"),
                            rs.getString("credible_ai_answer"),
                            rs.getString("target_ai_answer"),
                            toKst(rs.getTimestamp("submitted_at")),
                            rs.getString("memo"));
                },
                params.toArray());

        Integer memoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wrong_answer_notes WHERE user_id = ?", Integer.class, userId);
        return new WrongAnswersResponse(items, memoCount == null ? 0 : memoCount);
    }

    /**
     * 틀린 적 있는 문제에만 메모를 남긴다. 이미 있으면 덮어쓴다.
     */
    @Transactional
    public MemoResponse saveMemo(Long userId, Long questionId, String rawMemo) {
        Integer wrongCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM answers WHERE user_id = ? AND question_id = ? AND is_correct = FALSE",
                Integer.class, userId, questionId);
        if (wrongCount == null || wrongCount == 0) {
            throw new BusinessException(WrongAnswerErrorCode.WRONG_ANSWER_NOT_FOUND);
        }

        String memo = rawMemo.strip();
        LocalDateTime now = KstDateTime.nowUtc();
        jdbcTemplate.update(
                """
                INSERT INTO wrong_answer_notes (user_id, question_id, memo, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE memo = VALUES(memo), updated_at = VALUES(updated_at)
                """,
                userId, questionId, memo, now, now);
        return new MemoResponse(questionId, memo);
    }

    private static OffsetDateTime toKst(Timestamp utc) {
        if (utc == null) {
            return null;
        }
        return utc.toLocalDateTime().atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(KstDateTime.KST).toOffsetDateTime();
    }
}
