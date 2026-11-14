package com.unid8.damideo.user;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceId;
import com.unid8.damideo.common.time.KstDateTime;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MyRecordApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 막 가입한 사용자의 홈: 데일리 시작 전, 연속 0, 진행 중인 판 없음
    @Test
    void home_newUser() throws Exception {
        String device = UUID.randomUUID().toString();
        newUser(device);

        get(device, "/api/home")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HOME_SUCCESS"))
                .andExpect(jsonPath("$.data.daily.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.daily.solvedCount").value(0))
                .andExpect(jsonPath("$.data.streak.current").value(0))
                .andExpect(jsonPath("$.data.inProgressGameId").isEmpty())
                .andExpect(jsonPath("$.data.bestWinStreak").value(0));
    }

    // 끝내지 않은 판이 있으면 그 판 id를 준다
    @Test
    void home_inProgressGame() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        Long gameId = newGame(userId, "NORMAL", "IN_PROGRESS");

        get(device, "/api/home").andExpect(jsonPath("$.data.inProgressGameId").value(gameId));
    }

    // 최근 28일을 빠짐없이, 데일리를 다 푼 날은 completed와 맞힌 수를 준다
    @Test
    void dailyCalendar_last28Days() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        LocalDate day = KstDateTime.today().minusDays(3);
        Long q1 = newQuestion("EVIDENCE");
        Long q2 = newQuestion("LOGIC");
        newDailySet(day, q1, q2);
        addDailyAnswer(userId, q1, day, true);
        addDailyAnswer(userId, q2, day, false);

        get(device, "/api/users/me/daily-calendar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(28))
                .andExpect(jsonPath("$.data.days[27].date").value(KstDateTime.today().toString()))
                .andExpect(jsonPath("$.data.days[24].date").value(day.toString()))
                .andExpect(jsonPath("$.data.days[24].completed").value(true))
                .andExpect(jsonPath("$.data.days[24].correctCount").value(1))
                .andExpect(jsonPath("$.data.days[23].completed").value(false));
    }

    // 3회 미만이면 성장 기록이 잠겨 있다
    @Test
    void growth_lockedUnderThreeSessions() throws Exception {
        String device = UUID.randomUUID().toString();
        newUser(device);

        get(device, "/api/users/me/growth")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.remainingSessions").value(3))
                .andExpect(jsonPath("$.data.scoreTrend.length()").value(0));
    }

    // 점수 추이는 약점 재도전을 빼고, 계열별 정답률은 처음과 최근을 겹치지 않게 비교한다 (4회면 2회 vs 2회)
    @Test
    void growth_trendAndCategoryAccuracy() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        LocalDateTime base = LocalDateTime.of(2026, 11, 10, 3, 0);
        Long g1 = newGame(userId, "NORMAL", "COMPLETED");
        addGameAnswer(userId, g1, newQuestion("EVIDENCE"), false, base);
        addGameAnswer(userId, g1, newQuestion("EVIDENCE"), false, base);
        Long g2 = newGame(userId, "WEAKNESS_RETRY", "COMPLETED");
        addGameAnswer(userId, g2, newQuestion("EVIDENCE"), true, base.plusDays(1));
        Long g3 = newGame(userId, "NORMAL", "COMPLETED");
        addGameAnswer(userId, g3, newQuestion("EVIDENCE"), true, base.plusDays(2));
        Long g4 = newGame(userId, "NORMAL", "COMPLETED");
        addGameAnswer(userId, g4, newQuestion("EVIDENCE"), true, base.plusDays(3));
        newGame(userId, "NORMAL", "IN_PROGRESS");
        jdbcTemplate.update("INSERT INTO user_type_history (user_id, type_code, recorded_date) VALUES (?, 'EVIDENCE', ?)",
                userId, Date.valueOf(LocalDate.of(2026, 11, 10)));

        get(device, "/api/users/me/growth")
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.remainingSessions").value(0))
                .andExpect(jsonPath("$.data.scoreTrend.length()").value(3))
                .andExpect(jsonPath("$.data.scoreTrend[0].score").value(0))
                .andExpect(jsonPath("$.data.scoreTrend[0].total").value(2))
                .andExpect(jsonPath("$.data.scoreTrend[0].sessionType").value("GAME"))
                .andExpect(jsonPath("$.data.categoryAccuracy[0].category").value("EVIDENCE"))
                .andExpect(jsonPath("$.data.categoryAccuracy[0].early").value(0.33))
                .andExpect(jsonPath("$.data.categoryAccuracy[0].recent").value(1.0))
                .andExpect(jsonPath("$.data.categoryAccuracy[1].early").isEmpty())
                .andExpect(jsonPath("$.data.typeHistory[0].code").value("EVIDENCE_TRUSTER"))
                .andExpect(jsonPath("$.data.typeHistory[0].name").value("근거 맹신형"));
    }

    // 3회면 첫 회와 마지막 회를 비교해서, 처음과 최근이 같은 값으로 나오지 않는다
    @Test
    void growth_threeSessionsCompareFirstAndLast() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        LocalDateTime base = LocalDateTime.of(2026, 11, 10, 3, 0);
        Long g1 = newGame(userId, "NORMAL", "COMPLETED");
        addGameAnswer(userId, g1, newQuestion("LOGIC"), false, base);
        Long g2 = newGame(userId, "NORMAL", "COMPLETED");
        addGameAnswer(userId, g2, newQuestion("LOGIC"), false, base.plusDays(1));
        Long g3 = newGame(userId, "NORMAL", "COMPLETED");
        addGameAnswer(userId, g3, newQuestion("LOGIC"), true, base.plusDays(2));

        get(device, "/api/users/me/growth")
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.categoryAccuracy[2].category").value("LOGIC"))
                .andExpect(jsonPath("$.data.categoryAccuracy[2].early").value(0.0))
                .andExpect(jsonPath("$.data.categoryAccuracy[2].recent").value(1.0));
    }

    private ResultActions get(String device, String url) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header(DeviceId.HEADER, device));
    }

    private Long newUser(String device) {
        return insert("INSERT INTO users (device_id, nickname) VALUES (?, ?)", device, "기록" + SEQ.incrementAndGet());
    }

    private Long newQuestion(String category) {
        return insert(
                """
                INSERT INTO questions
                    (category, tactic, question_type, prompt, credible_content, target_content,
                     explanation, check_question, source, difficulty)
                VALUES (?, 'FAKE_SOURCE', 'CHOICE_ANSWER', '질문', '믿을 보기', '골라낼 보기',
                        '해설', '확인 질문', 'https://example.com', 1)
                """,
                category);
    }

    private Long newGame(Long userId, String mode, String status) {
        return insert(
                "INSERT INTO game_sessions (user_id, game_mode, status, total_questions) VALUES (?, ?, ?, 10)",
                userId, mode, status);
    }

    private void newDailySet(LocalDate date, Long... questionIds) {
        Long setId = insert("INSERT INTO daily_sets (daily_date) VALUES (?)", Date.valueOf(date));
        for (int i = 0; i < questionIds.length; i++) {
            jdbcTemplate.update(
                    "INSERT INTO daily_set_questions (daily_set_id, question_id, seq, answer_pos) VALUES (?, ?, ?, 'A')",
                    setId, questionIds[i], i + 1);
        }
    }

    private void addDailyAnswer(Long userId, Long questionId, LocalDate date, boolean correct) {
        jdbcTemplate.update(
                """
                INSERT INTO answers (user_id, question_id, source, daily_date, selected_pos, is_correct, first_attempt)
                VALUES (?, ?, 'DAILY', ?, 'A', ?, TRUE)
                """,
                userId, questionId, Date.valueOf(date), correct);
    }

    private void addGameAnswer(Long userId, Long gameId, Long questionId, boolean correct, LocalDateTime at) {
        jdbcTemplate.update(
                """
                INSERT INTO answers (user_id, question_id, source, game_session_id, selected_pos, is_correct,
                                     first_attempt, submitted_at)
                VALUES (?, ?, 'GAME', ?, 'A', ?, TRUE, ?)
                """,
                userId, questionId, gameId, correct, at);
    }

    private Long insert(String sql, Object... args) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
