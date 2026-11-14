package com.unid8.damideo.streak;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceId;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StreakApiTest {

    private static final String TARGET = "골라낼 보기";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : new String[]{"answers", "streak_challenges", "user_type_history",
                        "game_session_questions", "game_sessions", "daily_set_questions", "daily_sets",
                        "questions", "users"}) {
                    statement.execute("TRUNCATE TABLE " + table);
                }
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }

    // 시작하면 첫 문제(1번)와 지금 최고 기록을 준다
    @Test
    void start_returnsFirstQuestion() throws Exception {
        String device = newUser("시작");
        addQuestions(3);

        start(device)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("STREAK_CHALLENGE_STARTED"))
                .andExpect(jsonPath("$.data.challengeId").isNumber())
                .andExpect(jsonPath("$.data.bestWinStreak").value(0))
                .andExpect(jsonPath("$.data.question.seq").value(1));
    }

    // 맞히면 연승이 이어지고 최고 기록이 바로 갱신된다
    @Test
    void correctAnswer_continuesAndUpdatesBest() throws Exception {
        String device = newUser("정답");
        addQuestions(3);
        String started = body(start(device));

        answer(device, challengeId(started), questionId(started, "$.data.question"), correctPos(started, "$.data.question"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reveal.correct").value(true))
                .andExpect(jsonPath("$.data.currentWinStreak").value(1))
                .andExpect(jsonPath("$.data.bestWinStreak").value(1))
                .andExpect(jsonPath("$.data.newRecord").value(true))
                .andExpect(jsonPath("$.data.ended").value(false))
                .andExpect(jsonPath("$.data.nextQuestion.seq").value(2));
    }

    // 틀리면 끝나고, 끝난 도전에는 다시 답할 수 없다
    @Test
    void wrongAnswer_endsChallenge() throws Exception {
        String device = newUser("오답");
        addQuestions(3);
        String started = body(start(device));
        long challengeId = challengeId(started);
        long questionId = questionId(started, "$.data.question");

        answer(device, challengeId, questionId, wrongPos(started, "$.data.question"))
                .andExpect(jsonPath("$.data.reveal.correct").value(false))
                .andExpect(jsonPath("$.data.ended").value(true))
                .andExpect(jsonPath("$.data.endReason").value("WRONG"))
                .andExpect(jsonPath("$.data.nextQuestion").isEmpty());

        answer(device, challengeId, questionId, "A")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_ENDED"));
    }

    // 낼 문제를 다 맞히면 완주로 끝난다
    @Test
    void allCorrect_completesChallenge() throws Exception {
        String device = newUser("완주");
        addQuestions(2);
        String started = body(start(device));
        long challengeId = challengeId(started);

        String first = body(answer(device, challengeId, questionId(started, "$.data.question"),
                correctPos(started, "$.data.question")));
        answer(device, challengeId, questionId(first, "$.data.nextQuestion"), correctPos(first, "$.data.nextQuestion"))
                .andExpect(jsonPath("$.data.ended").value(true))
                .andExpect(jsonPath("$.data.endReason").value("COMPLETED"))
                .andExpect(jsonPath("$.data.currentWinStreak").value(2))
                .andExpect(jsonPath("$.data.bestWinStreak").value(2));
    }

    // 지금 낸 문제가 아니면 400
    @Test
    void answer_notCurrentQuestion() throws Exception {
        String device = newUser("다른문제");
        addQuestions(2);
        String started = body(start(device));

        answer(device, challengeId(started), questionId(started, "$.data.question") + 999, "A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_CURRENT_QUESTION"));
    }

    // 다른 사람의 도전이면 404
    @Test
    void answer_otherUsersChallenge() throws Exception {
        String owner = newUser("주인");
        String other = newUser("남");
        addQuestions(2);
        String started = body(start(owner));

        answer(other, challengeId(started), questionId(started, "$.data.question"), "A")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_NOT_FOUND"));
    }

    // 다시 시작하면 진행 중이던 도전은 끝난다
    @Test
    void startAgain_endsRunningChallenge() throws Exception {
        String device = newUser("재시작");
        addQuestions(3);
        String first = body(start(device));
        start(device).andExpect(status().isOk());

        answer(device, challengeId(first), questionId(first, "$.data.question"), "A")
                .andExpect(status().isConflict());
    }

    // 낼 문제가 없으면 409
    @Test
    void start_withoutQuestions() throws Exception {
        String device = newUser("빈문제");

        start(device)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_QUESTION_AVAILABLE"));
    }

    // 최고 연승 내림차순, 같으면 먼저 달성한 순이고 동점은 같은 순위. 기록이 없으면 내 순위는 null
    @Test
    void ranking_ordersAndSharesTiedRank() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 11, 14, 10, 0);
        newUserWithBest("늦은7", 7, base.plusHours(1));
        newUserWithBest("빠른7", 7, base);
        newUserWithBest("오", 5, base);
        String me = newUser("기록없음");

        mockMvc.perform(get("/api/rankings").header(DeviceId.HEADER, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RANKING_SUCCESS"))
                .andExpect(jsonPath("$.data.top.length()").value(3))
                .andExpect(jsonPath("$.data.top[0].nickname").value("빠른7"))
                .andExpect(jsonPath("$.data.top[0].rank").value(1))
                .andExpect(jsonPath("$.data.top[1].rank").value(1))
                .andExpect(jsonPath("$.data.top[2].rank").value(3))
                .andExpect(jsonPath("$.data.me.rank").isEmpty())
                .andExpect(jsonPath("$.data.me.nickname").value("기록없음"));
    }

    private ResultActions start(String device) throws Exception {
        return mockMvc.perform(post("/api/streak-challenges").header(DeviceId.HEADER, device));
    }

    private ResultActions answer(String device, long challengeId, long questionId, String pos) throws Exception {
        return mockMvc.perform(post("/api/streak-challenges/{id}/answers", challengeId)
                .header(DeviceId.HEADER, device)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"questionId": %d, "selectedPos": "%s"}
                        """.formatted(questionId, pos)));
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static long challengeId(String json) {
        return ((Number) JsonPath.read(json, "$.data.challengeId")).longValue();
    }

    private static long questionId(String json, String question) {
        return ((Number) JsonPath.read(json, question + ".questionId")).longValue();
    }

    private static String correctPos(String json, String question) {
        return TARGET.equals(JsonPath.read(json, question + ".optionA")) ? "A" : "B";
    }

    private static String wrongPos(String json, String question) {
        return "A".equals(correctPos(json, question)) ? "B" : "A";
    }

    private String newUser(String nickname) {
        String device = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO users (device_id, nickname) VALUES (?, ?)", device, nickname);
        return device;
    }

    private void newUserWithBest(String nickname, int best, LocalDateTime at) {
        jdbcTemplate.update(
                "INSERT INTO users (device_id, nickname, best_streak_win, best_streak_win_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), nickname, best, at);
    }

    private void addQuestions(int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    """
                    INSERT INTO questions
                        (category, tactic, question_type, prompt, credible_content, target_content,
                         explanation, check_question, source, difficulty)
                    VALUES ('EVIDENCE', 'FAKE_SOURCE', 'CHOICE_ANSWER', ?, '믿을 보기', ?,
                            '해설', '확인 질문', 'https://example.com', 1)
                    """,
                    "연승 문제 " + i, TARGET);
        }
    }
}
