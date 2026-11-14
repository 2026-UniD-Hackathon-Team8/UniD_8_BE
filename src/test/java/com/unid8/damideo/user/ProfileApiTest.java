package com.unid8.damideo.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceId;
import com.unid8.damideo.common.time.KstDateTime;
import java.sql.Date;
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
class ProfileApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 닉네임을 바꾸면 바뀐 닉네임을 돌려준다
    @Test
    void changeNickname_success() throws Exception {
        String deviceId = signUp(nickname());
        String newNickname = nickname();

        changeNickname(deviceId, newNickname)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NICKNAME_UPDATED"))
                .andExpect(jsonPath("$.data.nickname").value(newNickname));
    }

    // 다른 사람이 쓰는 닉네임이면 409
    @Test
    void changeNickname_duplicated() throws Exception {
        String taken = nickname();
        signUp(taken);
        String deviceId = signUp(nickname());

        changeNickname(deviceId, taken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_DUPLICATED"));
    }

    // 지금 내 닉네임과 같으면 그대로 성공
    @Test
    void changeNickname_sameAsMine() throws Exception {
        String mine = nickname();
        String deviceId = signUp(mine);

        changeNickname(deviceId, mine).andExpect(status().isOk());
    }

    // 길이가 맞지 않으면 400
    @Test
    void changeNickname_invalidLength() throws Exception {
        String deviceId = signUp(nickname());

        changeNickname(deviceId, "가")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nickname").exists());
    }

    // 막 가입한 사용자는 유형 분석 중이고 기록이 0
    @Test
    void profile_newUser_isAnalyzing() throws Exception {
        String mine = nickname();
        String deviceId = signUp(mine);

        profile(deviceId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROFILE_SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value(mine))
                .andExpect(jsonPath("$.data.streak.current").value(0))
                .andExpect(jsonPath("$.data.streak.completedToday").value(false))
                .andExpect(jsonPath("$.data.bestWinStreak").value(0))
                .andExpect(jsonPath("$.data.weakType.status").value("ANALYZING"))
                .andExpect(jsonPath("$.data.weakType.code").isEmpty())
                .andExpect(jsonPath("$.data.weakType.progress.solved").value(0))
                .andExpect(jsonPath("$.data.weakType.progress.required").value(20));
    }

    // 유형이 정해졌으면 유형 카드 정보와 가장 강한 계열을 보여 준다
    @Test
    void profile_decidedType() throws Exception {
        String deviceId = signUp(nickname());
        Long userId = userId(deviceId);
        addAnswers(userId, "EVIDENCE", 3, 4);
        addAnswers(userId, "CONTEXT", 3, 4);
        addAnswers(userId, "LOGIC", 1, 4);
        addAnswers(userId, "TIMELINE", 2, 4);
        addAnswers(userId, "QUESTION", 4, 4);
        jdbcTemplate.update("UPDATE users SET current_type = 'LOGIC', best_streak_win = 7 WHERE id = ?", userId);

        profile(deviceId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bestWinStreak").value(7))
                .andExpect(jsonPath("$.data.weakType.status").value("DECIDED"))
                .andExpect(jsonPath("$.data.weakType.code").value("LOGIC_ILLUSION"))
                .andExpect(jsonPath("$.data.weakType.name").value("논리 착시형"))
                .andExpect(jsonPath("$.data.weakType.weakCategory").value("LOGIC"))
                .andExpect(jsonPath("$.data.weakType.strongCategory").value("QUESTION"))
                .andExpect(jsonPath("$.data.weakType.checkQuestion").value("따져 보면 말이 되나?"))
                .andExpect(jsonPath("$.data.weakType.progress.solved").value(20));
    }

    // 탐정형은 약한 계열이 없다
    @Test
    void profile_detective() throws Exception {
        String deviceId = signUp(nickname());
        jdbcTemplate.update("UPDATE users SET current_type = 'DETECTIVE' WHERE id = ?", userId(deviceId));

        profile(deviceId)
                .andExpect(jsonPath("$.data.weakType.code").value("DETECTIVE"))
                .andExpect(jsonPath("$.data.weakType.name").value("탐정형"))
                .andExpect(jsonPath("$.data.weakType.weakCategory").isEmpty());
    }

    // 어제까지 이어졌으면 연속 기록을 보여 주고, 이틀 전에 끊겼으면 0
    @Test
    void profile_streakResetsWhenBroken() throws Exception {
        String deviceId = signUp(nickname());
        Long userId = userId(deviceId);

        setStreak(userId, 3, 5, 1);
        profile(deviceId)
                .andExpect(jsonPath("$.data.streak.current").value(3))
                .andExpect(jsonPath("$.data.streak.longest").value(5))
                .andExpect(jsonPath("$.data.streak.completedToday").value(false));

        setStreak(userId, 3, 5, 0);
        profile(deviceId).andExpect(jsonPath("$.data.streak.completedToday").value(true));

        setStreak(userId, 3, 5, 2);
        profile(deviceId).andExpect(jsonPath("$.data.streak.current").value(0));
    }

    // 헤더 없이 부르면 401
    @Test
    void profile_withoutDeviceId() throws Exception {
        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    private String signUp(String nickname) throws Exception {
        String deviceId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId": "%s", "nickname": "%s"}
                                """.formatted(deviceId, nickname)))
                .andExpect(status().isOk());
        return deviceId;
    }

    private Long userId(String deviceId) {
        return userRepository.findByDeviceId(deviceId).orElseThrow().getId();
    }

    private ResultActions changeNickname(String deviceId, String nickname) throws Exception {
        return mockMvc.perform(patch("/api/users/me/nickname")
                .header(DeviceId.HEADER, deviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nickname": "%s"}
                        """.formatted(nickname)));
    }

    private ResultActions profile(String deviceId) throws Exception {
        return mockMvc.perform(get("/api/users/me/profile").header(DeviceId.HEADER, deviceId));
    }

    private void setStreak(Long userId, int current, int longest, int daysAgo) {
        jdbcTemplate.update(
                "UPDATE users SET current_streak = ?, longest_streak = ?, last_completed_date = ? WHERE id = ?",
                current, longest, Date.valueOf(KstDateTime.today().minusDays(daysAgo)), userId);
    }

    /** 계열마다 total개를 처음 풀어서 correct개를 맞힌 기록을 넣는다 */
    private void addAnswers(Long userId, String category, int correct, int total) {
        for (int i = 0; i < total; i++) {
            Long questionId = insertQuestion(category);
            jdbcTemplate.update(
                    """
                    INSERT INTO answers (user_id, question_id, source, selected_pos, is_correct, first_attempt, submitted_at)
                    VALUES (?, ?, 'GAME', 'A', ?, TRUE, ?)
                    """,
                    userId, questionId, i < correct, LocalDateTime.now().minusMinutes(total - i));
        }
    }

    private Long insertQuestion(String category) {
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
        return keyHolder.getKey().longValue();
    }

    private static String nickname() {
        return "프로필" + SEQ.incrementAndGet();
    }
}
