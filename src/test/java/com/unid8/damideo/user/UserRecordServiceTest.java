package com.unid8.damideo.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.time.KstDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRecordServiceTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    UserRecordService userRecordService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 데일리를 처음 마치면 연속 기록이 1이 된다
    @Test
    void completeDaily_startsStreak() {
        Long userId = newUser();
        LocalDate today = KstDateTime.today();

        userRecordService.completeDaily(userId, today);

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getCurrentStreak()).isEqualTo(1);
        assertThat(user.getLongestStreak()).isEqualTo(1);
        assertThat(user.getLastCompletedDate()).isEqualTo(today);
    }

    // 어제에 이어서 마치면 1 늘고, 같은 날 다시 불려도 그대로다
    @Test
    void completeDaily_continuesStreakOnce() {
        Long userId = newUser();
        LocalDate today = KstDateTime.today();

        userRecordService.completeDaily(userId, today.minusDays(1));
        userRecordService.completeDaily(userId, today);
        userRecordService.completeDaily(userId, today);

        assertThat(userRepository.findById(userId).orElseThrow().getCurrentStreak()).isEqualTo(2);
    }

    // 처음 푼 답이 계열마다 4문제가 안 되면 유형을 정하지 않는다
    @Test
    void completeGame_keepsAnalyzingUntilEnoughAnswers() {
        Long userId = newUser();
        addAnswers(userId, "EVIDENCE", 3, 4);
        addAnswers(userId, "CONTEXT", 3, 4);

        userRecordService.completeGame(userId, null);

        assertThat(userRepository.findById(userId).orElseThrow().getCurrentType()).isNull();
        assertThat(historyCount(userId)).isZero();
    }

    // 계열마다 4문제를 넘기면 가장 약한 계열로 유형을 정하고 이력을 남긴다
    @Test
    void completeGame_decidesTypeAndRecordsHistory() {
        Long userId = newUser();
        addAnswers(userId, "EVIDENCE", 4, 4);
        addAnswers(userId, "CONTEXT", 3, 4);
        addAnswers(userId, "LOGIC", 3, 4);
        addAnswers(userId, "TIMELINE", 1, 4);
        addAnswers(userId, "QUESTION", 4, 4);

        userRecordService.completeGame(userId, null);
        userRecordService.completeGame(userId, null);

        assertThat(userRepository.findById(userId).orElseThrow().getCurrentType()).isEqualTo("TIMELINE");
        assertThat(historyCount(userId)).isEqualTo(1);
    }

    private Long newUser() {
        int seq = SEQ.incrementAndGet();
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO users (device_id, nickname) VALUES (?, ?)", new String[]{"id"});
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, "기록" + seq);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private int historyCount(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_type_history WHERE user_id = ?", Integer.class, userId);
    }

    /** 계열마다 total개를 처음 풀어서 correct개를 맞힌 기록을 넣는다 */
    private void addAnswers(Long userId, String category, int correct, int total) {
        for (int i = 0; i < total; i++) {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement(
                        """
                        INSERT INTO questions
                            (category, tactic, question_type, prompt, credible_content, target_content,
                             explanation, check_question, source, difficulty)
                        VALUES (?, 'FAKE_SOURCE', 'CHOICE_ANSWER', '질문', '믿을 쪽', '골라낼 쪽',
                                '해설', '확인 질문', 'https://example.com', 1)
                        """,
                        new String[]{"id"});
                statement.setString(1, category);
                return statement;
            }, keyHolder);
            jdbcTemplate.update(
                    """
                    INSERT INTO answers (user_id, question_id, source, selected_pos, is_correct, first_attempt, submitted_at)
                    VALUES (?, ?, 'GAME', 'A', ?, TRUE, ?)
                    """,
                    userId, keyHolder.getKey().longValue(), i < correct, LocalDateTime.now().minusMinutes(total - i));
        }
    }
}
