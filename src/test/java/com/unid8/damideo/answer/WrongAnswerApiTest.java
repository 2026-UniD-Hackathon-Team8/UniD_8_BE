package com.unid8.damideo.answer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceId;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WrongAnswerApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 14, 3, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 틀린 답만 최신순으로, 같은 문제는 최신 1건만 보여 준다
    @Test
    void list_latestWrongAnswerPerQuestion() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        Long logic = newQuestion("LOGIC");
        Long timeline = newQuestion("TIMELINE");
        Long evidence = newQuestion("EVIDENCE");
        addAnswer(userId, logic, false, BASE);
        addAnswer(userId, timeline, false, BASE.plusMinutes(1));
        addAnswer(userId, logic, false, BASE.plusMinutes(2));
        addAnswer(userId, evidence, true, BASE.plusMinutes(3));

        list(device, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WRONG_ANSWERS_SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].questionId").value(logic))
                .andExpect(jsonPath("$.data.items[0].myChoiceText").value("믿을 보기"))
                .andExpect(jsonPath("$.data.items[0].answerChoiceText").value("골라낼 보기"))
                .andExpect(jsonPath("$.data.items[0].categoryName").value("논리"))
                .andExpect(jsonPath("$.data.items[0].memo").isEmpty())
                .andExpect(jsonPath("$.data.items[1].questionId").value(timeline))
                .andExpect(jsonPath("$.data.memoCount").value(0));
    }

    // 계열로 거를 수 있다
    @Test
    void list_filterByCategory() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        addAnswer(userId, newQuestion("LOGIC"), false, BASE);
        Long timeline = newQuestion("TIMELINE");
        addAnswer(userId, timeline, false, BASE.plusMinutes(1));

        list(device, "TIMELINE")
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].questionId").value(timeline));
    }

    // 틀린 적 없으면 빈 목록
    @Test
    void list_empty() throws Exception {
        String device = UUID.randomUUID().toString();
        newUser(device);

        list(device, null).andExpect(jsonPath("$.data.items.length()").value(0));
    }

    // 메모를 저장하고 다시 저장하면 덮어쓴다. 오답노트에 함께 나온다
    @Test
    void saveMemo_overwrites() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        Long question = newQuestion("CONTEXT");
        addAnswer(userId, question, false, BASE);

        saveMemo(device, question, "숫자가 구체적이어서")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMO_SAVED"));
        saveMemo(device, question, "그냥 감")
                .andExpect(jsonPath("$.data.memo").value("그냥 감"));

        list(device, null)
                .andExpect(jsonPath("$.data.items[0].memo").value("그냥 감"))
                .andExpect(jsonPath("$.data.memoCount").value(1));
    }

    // 틀린 적 없는 문제에는 메모를 남길 수 없다
    @Test
    void saveMemo_notWrongAnswer() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        Long question = newQuestion("CONTEXT");
        addAnswer(userId, question, true, BASE);

        saveMemo(device, question, "메모")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WRONG_ANSWER_NOT_FOUND"));
    }

    // 빈 메모는 400
    @Test
    void saveMemo_blank() throws Exception {
        String device = UUID.randomUUID().toString();
        Long userId = newUser(device);
        Long question = newQuestion("CONTEXT");
        addAnswer(userId, question, false, BASE);

        saveMemo(device, question, " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.memo").exists());
    }

    private ResultActions list(String device, String category) throws Exception {
        var request = get("/api/users/me/wrong-answers").header(DeviceId.HEADER, device);
        if (category != null) {
            request.param("category", category);
        }
        return mockMvc.perform(request);
    }

    private ResultActions saveMemo(String device, Long questionId, String memo) throws Exception {
        return mockMvc.perform(put("/api/users/me/wrong-answers/{id}/memo", questionId)
                .header(DeviceId.HEADER, device)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"memo": "%s"}
                        """.formatted(memo)));
    }

    private Long newUser(String device) {
        return insert("INSERT INTO users (device_id, nickname) VALUES (?, ?)", device, "오답" + SEQ.incrementAndGet());
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

    private void addAnswer(Long userId, Long questionId, boolean correct, LocalDateTime at) {
        jdbcTemplate.update(
                """
                INSERT INTO answers (user_id, question_id, source, selected_pos, is_correct, first_attempt, submitted_at)
                VALUES (?, ?, 'GAME', 'A', ?, FALSE, ?)
                """,
                userId, questionId, correct, at);
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
