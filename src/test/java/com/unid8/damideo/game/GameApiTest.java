package com.unid8.damideo.game;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceId;
import com.unid8.damideo.common.auth.DeviceUserReader;
import com.unid8.damideo.question.QuestionCategory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, GameApiTest.TestAuthConfig.class})
class GameApiTest {

    private static final String DEVICE_ID = "22222222-2222-4222-8222-222222222222";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE answers");
        jdbcTemplate.execute("TRUNCATE TABLE game_session_questions");
        jdbcTemplate.execute("TRUNCATE TABLE game_sessions");
        jdbcTemplate.execute("TRUNCATE TABLE daily_set_questions");
        jdbcTemplate.execute("TRUNCATE TABLE daily_sets");
        jdbcTemplate.execute("TRUNCATE TABLE questions");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        jdbcTemplate.update(
                "INSERT INTO users (device_id, nickname) VALUES (?, ?)",
                DEVICE_ID, "게임테스터"
        );

        String[] tactics = {"FAKE_SOURCE", "OUT_OF_CONTEXT", "FALSE_CAUSALITY"};
        for (QuestionCategory cat : QuestionCategory.values()) {
            for (int i = 0; i < 3; i++) {
                jdbcTemplate.update(
                        """
                        INSERT INTO questions
                            (category, tactic, question_type, prompt, credible_content, target_content,
                             explanation, check_question, source, difficulty, is_daily_eligible, is_active)
                        VALUES (?, ?, 'CHOICE_ANSWER', ?, '신뢰 보기', '가짜 보기',
                                '해설입니다', '확인질문입니다', 'https://example.com', ?, 1, 1)
                        """,
                        cat.name(), tactics[i], cat.displayName() + " 문제 " + i, (i % 3) + 1
                );
            }
        }
    }

    @Test
    void startGameResumeSubmitAllAndGetResult() throws Exception {
        // 1. 첫 판 시작 (10문제)
        MvcResult firstGame = mockMvc.perform(post("/api/games")
                        .header(DeviceId.HEADER, DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameMode": "NORMAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(10))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.questions", hasSize(10)))
                .andReturn();

        long firstSessionId = objectMapper.readTree(firstGame.getResponse().getContentAsString())
                .path("data").path("gameSessionId").asLong();

        // 2. 새 판을 시작하면 기존 판은 ABANDONED 되고 새 판이 current로 조회됨
        MvcResult secondGame = mockMvc.perform(post("/api/games")
                        .header(DeviceId.HEADER, DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameMode": "NORMAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(10))
                .andReturn();

        JsonNode secondData = objectMapper.readTree(secondGame.getResponse().getContentAsString()).path("data");
        long secondSessionId = secondData.path("gameSessionId").asLong();
        JsonNode questions = secondData.path("questions");

        String firstStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM game_sessions WHERE id = ?", String.class, firstSessionId
        );
        org.assertj.core.api.Assertions.assertThat(firstStatus).isEqualTo("ABANDONED");

        // 3. 1번 문제만 풀고 /current 이어하기 확인
        long q1 = questions.get(0).path("questionId").asLong();
        mockMvc.perform(post("/api/games/{id}/answers", secondSessionId)
                        .header(DeviceId.HEADER, DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId": %d, "selectedPos": "A"}
                                """.formatted(q1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(false));

        mockMvc.perform(get("/api/games/current").header(DeviceId.HEADER, DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gameSessionId").value(secondSessionId))
                .andExpect(jsonPath("$.data.solvedCount").value(1))
                .andExpect(jsonPath("$.data.nextSeq").value(2));

        // 4. 나머지 9문제 제출 -> 마지막 제출 시 completed=true
        for (int i = 1; i < questions.size(); i++) {
            long qId = questions.get(i).path("questionId").asLong();
            boolean expectDone = (i == questions.size() - 1);
            mockMvc.perform(post("/api/games/{id}/answers", secondSessionId)
                            .header(DeviceId.HEADER, DEVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"questionId": %d, "selectedPos": "B"}
                                    """.formatted(qId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.completed").value(expectDone));
        }

        // 5. 판 결과 조회
        mockMvc.perform(get("/api/games/{id}/result", secondSessionId)
                        .header(DeviceId.HEADER, DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gameSessionId").value(secondSessionId))
                .andExpect(jsonPath("$.data.totalCount").value(10))
                .andExpect(jsonPath("$.data.questions", hasSize(10)));
    }

    @TestConfiguration
    static class TestAuthConfig {
        @Bean
        DeviceUserReader deviceUserReader(JdbcTemplate jdbcTemplate) {
            return deviceId -> jdbcTemplate.query(
                    "SELECT id FROM users WHERE device_id = ? LIMIT 1",
                    rs -> rs.next() ? Optional.of(rs.getLong("id")) : Optional.empty(),
                    deviceId.toString()
            );
        }
    }
}
