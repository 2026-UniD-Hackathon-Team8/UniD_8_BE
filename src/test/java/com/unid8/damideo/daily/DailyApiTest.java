package com.unid8.damideo.daily;

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
import java.util.UUID;
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
@Import({TestcontainersConfiguration.class, DailyApiTest.TestAuthConfig.class})
class DailyApiTest {

    private static final String DEVICE_ID = "11111111-1111-4111-8111-111111111111";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private Long userId;

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
                "INSERT INTO users (device_id, nickname) VALUES (?, ?)",
                DEVICE_ID, "테스터"
        );
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE device_id = ?", Long.class, DEVICE_ID
        );

        for (QuestionCategory cat : QuestionCategory.values()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO questions
                        (category, tactic, question_type, prompt, credible_content, target_content,
                         explanation, check_question, source, difficulty, is_daily_eligible, is_active)
                    VALUES (?, 'FAKE_SOURCE', 'CHOICE_ANSWER', ?, '신뢰 보기', '가짜 보기',
                            '해설입니다', '확인질문입니다', 'https://example.com', 1, 1, 1)
                    """,
                    cat.name(), cat.displayName() + " 문제 지문"
            );
        }
    }

    // 오늘의_데일리를_조회하고_5문제를_모두_풀면_결과를_조회할_수_있다
    @Test
    void getTodayDailySubmitAllAndGetResult() throws Exception {
        // 1. 데일리 조회: 5문제 반환 및 정답/해설 미포함 확인
        MvcResult dailyRes = mockMvc.perform(get("/api/daily").header(DeviceId.HEADER, DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.questions", hasSize(5)))
                .andExpect(jsonPath("$.data.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].correctPos").doesNotExist())
                .andReturn();

        // 2. 완료 전 결과 조회 시 400 DAILY_NOT_COMPLETED
        mockMvc.perform(get("/api/daily/result").header(DeviceId.HEADER, DEVICE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DAILY_NOT_COMPLETED"));

        JsonNode questions = objectMapper.readTree(dailyRes.getResponse().getContentAsString())
                .path("data").path("questions");

        // 3. 첫 문제 제출 및 중복 제출 멱등성 확인
        long firstQId = questions.get(0).path("questionId").asLong();
        MvcResult firstSubmit = mockMvc.perform(post("/api/daily/answers")
                        .header(DeviceId.HEADER, DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId": %d, "selectedPos": "A"}
                                """.formatted(firstQId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPos").value("A"))
                .andExpect(jsonPath("$.data.tactic").value("FAKE_SOURCE"))
                .andExpect(jsonPath("$.data.tacticName").value("가짜 출처"))
                .andExpect(jsonPath("$.data.explanation").value("해설입니다"))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andReturn();

        String firstCorrectPos = objectMapper.readTree(firstSubmit.getResponse().getContentAsString())
                .path("data").path("correctPos").asText();

        // 다른 선택지("B")로 재제출해도 처음 답("A")만 인정
        mockMvc.perform(post("/api/daily/answers")
                        .header(DeviceId.HEADER, DEVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId": %d, "selectedPos": "B"}
                                """.formatted(firstQId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPos").value("A"))
                .andExpect(jsonPath("$.data.correctPos").value(firstCorrectPos));

        // 4. 나머지 4문제 제출 -> 마지막 제출 시 completed=true
        for (int i = 1; i < questions.size(); i++) {
            long qId = questions.get(i).path("questionId").asLong();
            boolean expectDone = (i == questions.size() - 1);
            mockMvc.perform(post("/api/daily/answers")
                            .header(DeviceId.HEADER, DEVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"questionId": %d, "selectedPos": "A"}
                                    """.formatted(qId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.completed").value(expectDone));
        }

        // 5. 데일리 결과 조회
        mockMvc.perform(get("/api/daily/result").header(DeviceId.HEADER, DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.questions", hasSize(5)));
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
