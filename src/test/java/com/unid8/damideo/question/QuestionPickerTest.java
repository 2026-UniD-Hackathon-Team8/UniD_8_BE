package com.unid8.damideo.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.time.KstDateTime;
import java.sql.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuestionPickerTest {

    @Autowired
    QuestionPicker questionPicker;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long dailyQId;
    private Long unsolvedQId;
    private Long wrongQId;
    private Long correctQId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE answers");
        jdbcTemplate.execute("TRUNCATE TABLE daily_set_questions");
        jdbcTemplate.execute("TRUNCATE TABLE daily_sets");
        jdbcTemplate.execute("TRUNCATE TABLE questions");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        jdbcTemplate.update(
                "INSERT INTO users (device_id, nickname) VALUES ('33333333-3333-4333-8333-333333333333', '피커테스터')"
        );
        userId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", Long.class);

        dailyQId = insertQuestion("오늘 데일리");
        unsolvedQId = insertQuestion("안 푼 문제");
        wrongQId = insertQuestion("틀린 문제");
        correctQId = insertQuestion("맞힌 문제");

        jdbcTemplate.update("INSERT INTO daily_sets (daily_date) VALUES (?)", Date.valueOf(KstDateTime.today()));
        Long setId = jdbcTemplate.queryForObject("SELECT id FROM daily_sets LIMIT 1", Long.class);
        jdbcTemplate.update(
                "INSERT INTO daily_set_questions (daily_set_id, question_id, seq, answer_pos) VALUES (?, ?, 1, 'A')",
                setId, dailyQId
        );

        jdbcTemplate.update(
                "INSERT INTO answers (user_id, question_id, source, selected_pos, is_correct, first_attempt) VALUES (?, ?, 'GAME', 'A', 0, 1)",
                userId, wrongQId
        );
        jdbcTemplate.update(
                "INSERT INTO answers (user_id, question_id, source, selected_pos, is_correct, first_attempt) VALUES (?, ?, 'GAME', 'A', 1, 1)",
                userId, correctQId
        );
    }

    @Test
    void pickForStreakExcludesTodayDailyAndPrioritizesUnsolvedWrongCorrect() {
        Optional<Question> first = questionPicker.pickForStreak(userId, Set.of());
        assertThat(first).isPresent();
        assertThat(first.get().getId()).isEqualTo(unsolvedQId);

        Optional<Question> second = questionPicker.pickForStreak(userId, Set.of(unsolvedQId));
        assertThat(second).isPresent();
        assertThat(second.get().getId()).isEqualTo(wrongQId);

        Optional<Question> third = questionPicker.pickForStreak(userId, Set.of(unsolvedQId, wrongQId));
        assertThat(third).isPresent();
        assertThat(third.get().getId()).isEqualTo(correctQId);

        Optional<Question> exhausted = questionPicker.pickForStreak(userId, Set.of(unsolvedQId, wrongQId, correctQId));
        assertThat(exhausted).isEmpty();
    }

    private Long insertQuestion(String prompt) {
        jdbcTemplate.update(
                """
                INSERT INTO questions
                    (category, tactic, question_type, prompt, credible_content, target_content,
                     explanation, check_question, source, difficulty, is_daily_eligible, is_active)
                VALUES ('EVIDENCE', 'FAKE_SOURCE', 'CHOICE_ANSWER', ?, '신뢰', '가짜',
                        '해설', '확인질문', 'https://example.com', 1, 1, 1)
                """,
                prompt
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
