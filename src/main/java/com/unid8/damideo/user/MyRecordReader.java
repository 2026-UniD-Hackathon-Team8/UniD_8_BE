package com.unid8.damideo.user;

import com.unid8.damideo.common.time.KstDateTime;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 홈·달력·성장에 쓰는 기록 조회. 판·데일리 테이블은 읽기만 한다.
 */
@Component
@RequiredArgsConstructor
public class MyRecordReader {

    static final int DAILY_SIZE = 5;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 그날 데일리 문제 수. 세트가 아직 없으면 기본 5문제.
     */
    public int dailyTotal(LocalDate date) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM daily_set_questions dsq
                JOIN daily_sets ds ON ds.id = dsq.daily_set_id
                WHERE ds.daily_date = ?
                """,
                Integer.class, Date.valueOf(date));
        return count == null || count == 0 ? DAILY_SIZE : count;
    }

    public int dailySolvedCount(Long userId, LocalDate date) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM answers WHERE user_id = ? AND source = 'DAILY' AND daily_date = ?",
                Integer.class, userId, Date.valueOf(date));
        return count == null ? 0 : count;
    }

    public Optional<Long> inProgressGameId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM game_sessions WHERE user_id = ? AND status = 'IN_PROGRESS' ORDER BY id DESC LIMIT 1",
                Long.class, userId).stream().findFirst();
    }

    /**
     * 기간 안의 날짜별 데일리 기록. 푼 적 없는 날은 들어 있지 않다.
     */
    public Map<LocalDate, DailyDay> dailyDays(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, DailyDay> result = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT a.daily_date, COUNT(*) AS solved, SUM(CASE WHEN a.is_correct THEN 1 ELSE 0 END) AS correct
                FROM answers a
                WHERE a.user_id = ? AND a.source = 'DAILY' AND a.daily_date BETWEEN ? AND ?
                GROUP BY a.daily_date
                """,
                rs -> {
                    LocalDate date = rs.getDate("daily_date").toLocalDate();
                    result.put(date, new DailyDay(date, rs.getInt("solved"), rs.getInt("correct"), dailyTotal(date)));
                },
                userId, Date.valueOf(from), Date.valueOf(to));
        return result;
    }

    /**
     * 끝낸 판과 끝낸 데일리를 회 단위로, 오래된 순으로 돌려준다.
     */
    public List<Session> completedSessions(Long userId) {
        Map<Long, String> completedGames = new HashMap<>();
        jdbcTemplate.query(
                "SELECT id, game_mode FROM game_sessions WHERE user_id = ? AND status = 'COMPLETED'",
                rs -> {
                    completedGames.put(rs.getLong("id"), rs.getString("game_mode"));
                },
                userId);

        Map<String, Session> sessions = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT a.source, a.game_session_id, a.daily_date, a.is_correct, a.first_attempt, a.submitted_at,
                       q.category
                FROM answers a
                JOIN questions q ON q.id = a.question_id
                WHERE a.user_id = ? AND a.source IN ('GAME', 'DAILY')
                """,
                rs -> {
                    boolean game = "GAME".equals(rs.getString("source"));
                    String key;
                    Session session;
                    if (game) {
                        long gameId = rs.getLong("game_session_id");
                        if (!completedGames.containsKey(gameId)) {
                            return;
                        }
                        key = "G" + gameId;
                        session = sessions.computeIfAbsent(key, k -> new Session(
                                "GAME", "WEAKNESS_RETRY".equals(completedGames.get(gameId)), null));
                    } else {
                        LocalDate date = rs.getDate("daily_date").toLocalDate();
                        key = "D" + date;
                        session = sessions.computeIfAbsent(key, k -> new Session("DAILY", false, date));
                    }
                    session.add(rs.getString("category"), rs.getBoolean("is_correct"),
                            rs.getBoolean("first_attempt"), rs.getTimestamp("submitted_at").toLocalDateTime());
                },
                userId);

        List<Session> result = new ArrayList<>();
        for (Session session : sessions.values()) {
            if (session.isDaily() && session.answers.size() < dailyTotal(session.dailyDate)) {
                continue;
            }
            result.add(session);
        }
        result.sort(Comparator.comparing(Session::lastAnsweredAt));
        return result;
    }

    public List<TypeHistoryRow> typeHistory(Long userId) {
        return jdbcTemplate.query(
                "SELECT type_code, recorded_date FROM user_type_history WHERE user_id = ? ORDER BY recorded_date, id",
                (rs, rowNum) -> new TypeHistoryRow(rs.getString("type_code"), rs.getDate("recorded_date").toLocalDate()),
                userId);
    }

    public record DailyDay(LocalDate date, int solved, int correct, int total) {

        public boolean completed() {
            return solved >= total;
        }
    }

    public record TypeHistoryRow(String typeCode, LocalDate recordedDate) {
    }

    public record SessionAnswer(String category, boolean correct, boolean firstAttempt) {
    }

    /**
     * 판 한 번 또는 데일리 한 번.
     */
    public static final class Session {

        private final String type;
        private final boolean weaknessRetry;
        private final LocalDate dailyDate;
        private final List<SessionAnswer> answers = new ArrayList<>();
        private LocalDateTime lastAnsweredAt = LocalDateTime.MIN;

        Session(String type, boolean weaknessRetry, LocalDate dailyDate) {
            this.type = type;
            this.weaknessRetry = weaknessRetry;
            this.dailyDate = dailyDate;
        }

        void add(String category, boolean correct, boolean firstAttempt, LocalDateTime submittedAt) {
            answers.add(new SessionAnswer(category, correct, firstAttempt));
            if (submittedAt.isAfter(lastAnsweredAt)) {
                lastAnsweredAt = submittedAt;
            }
        }

        public String type() {
            return type;
        }

        public boolean isDaily() {
            return "DAILY".equals(type);
        }

        public boolean weaknessRetry() {
            return weaknessRetry;
        }

        public List<SessionAnswer> answers() {
            return answers;
        }

        public LocalDateTime lastAnsweredAt() {
            return lastAnsweredAt;
        }

        /**
         * 데일리는 그날, 판은 마지막으로 답한 시각의 KST 날짜.
         */
        public LocalDate date() {
            if (dailyDate != null) {
                return dailyDate;
            }
            return lastAnsweredAt.atOffset(ZoneOffset.UTC).atZoneSameInstant(KstDateTime.KST).toLocalDate();
        }

        public int correctCount() {
            return (int) answers.stream().filter(SessionAnswer::correct).count();
        }
    }
}
