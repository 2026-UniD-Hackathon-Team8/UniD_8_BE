package com.unid8.damideo.streak;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceId;
import com.unid8.damideo.common.time.KstDateTime;
import java.sql.Date;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DailyRankingApiTest {

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

    // 현재 연속 일수 내림차순, 같으면 최장 기록 순으로 보여 주고 동점은 같은 순위다
    @Test
    void ranking_ordersAndSharesTiedRank() throws Exception {
        newUserWithStreak("오일짧", 5, 5, 1);
        newUserWithStreak("오일길", 5, 9, 0);
        newUserWithStreak("삼일", 3, 3, 0);
        String me = newUserWithStreak("나", 0, 0, -1);

        mockMvc.perform(get("/api/rankings/daily").header(DeviceId.HEADER, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DAILY_RANKING_SUCCESS"))
                .andExpect(jsonPath("$.data.top.length()").value(3))
                .andExpect(jsonPath("$.data.top[0].nickname").value("오일길"))
                .andExpect(jsonPath("$.data.top[0].rank").value(1))
                .andExpect(jsonPath("$.data.top[0].currentStreak").value(5))
                .andExpect(jsonPath("$.data.top[0].longestStreak").value(9))
                .andExpect(jsonPath("$.data.top[1].nickname").value("오일짧"))
                .andExpect(jsonPath("$.data.top[1].rank").value(1))
                .andExpect(jsonPath("$.data.top[2].rank").value(3))
                .andExpect(jsonPath("$.data.me.rank").isEmpty())
                .andExpect(jsonPath("$.data.me.currentStreak").value(0));
    }

    // 마지막 완료일이 이틀 전이면 끊긴 기록이라 랭킹에서 빠진다
    @Test
    void ranking_excludesBrokenStreak() throws Exception {
        newUserWithStreak("끊김", 10, 10, 2);
        String me = newUserWithStreak("이어짐", 2, 2, 0);

        mockMvc.perform(get("/api/rankings/daily").header(DeviceId.HEADER, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.top.length()").value(1))
                .andExpect(jsonPath("$.data.top[0].nickname").value("이어짐"))
                .andExpect(jsonPath("$.data.me.rank").value(1))
                .andExpect(jsonPath("$.data.me.currentStreak").value(2));
    }

    // size보다 순위가 낮아도 내 순위는 따로 준다
    @Test
    void ranking_returnsMyRankOutsideTop() throws Exception {
        newUserWithStreak("일등", 9, 9, 0);
        newUserWithStreak("이등", 7, 7, 0);
        String me = newUserWithStreak("나", 4, 6, 1);

        mockMvc.perform(get("/api/rankings/daily").param("size", "1").header(DeviceId.HEADER, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.top.length()").value(1))
                .andExpect(jsonPath("$.data.me.rank").value(3))
                .andExpect(jsonPath("$.data.me.longestStreak").value(6));
    }

    /**
     * @param daysAgo 마지막 완료일이 며칠 전인지. 음수면 완료 기록 없음
     */
    private String newUserWithStreak(String nickname, int current, int longest, int daysAgo) {
        String device = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO users (device_id, nickname, current_streak, longest_streak, last_completed_date) VALUES (?, ?, ?, ?, ?)",
                device, nickname, current, longest,
                daysAgo < 0 ? null : Date.valueOf(KstDateTime.today().minusDays(daysAgo)));
        return device;
    }
}
