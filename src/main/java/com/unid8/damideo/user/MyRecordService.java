package com.unid8.damideo.user;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.stats.DailyStreak;
import com.unid8.damideo.stats.WeakType;
import com.unid8.damideo.user.MyRecordReader.DailyDay;
import com.unid8.damideo.user.MyRecordReader.Session;
import com.unid8.damideo.user.MyRecordReader.SessionAnswer;
import com.unid8.damideo.user.dto.DailyCalendarResponse;
import com.unid8.damideo.user.dto.DailyCalendarResponse.Day;
import com.unid8.damideo.user.dto.GrowthResponse;
import com.unid8.damideo.user.dto.GrowthResponse.CategoryAccuracy;
import com.unid8.damideo.user.dto.GrowthResponse.Score;
import com.unid8.damideo.user.dto.GrowthResponse.TypeRecord;
import com.unid8.damideo.user.dto.HomeResponse;
import com.unid8.damideo.user.dto.HomeResponse.Daily;
import com.unid8.damideo.user.dto.ProfileResponse.Streak;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈, 데일리 달력, 나의 성장.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyRecordService {

    static final int CALENDAR_DAYS = 28;
    static final int COMPARE_SESSIONS = 3;

    private final UserRepository userRepository;
    private final MyRecordReader reader;

    public HomeResponse getHome(Long userId) {
        User user = findUser(userId);
        LocalDate today = KstDateTime.today();

        int total = reader.dailyTotal(today);
        int solved = reader.dailySolvedCount(userId, today);
        String status = solved == 0 ? "NOT_STARTED" : solved >= total ? "COMPLETED" : "IN_PROGRESS";

        Streak streak = new Streak(
                DailyStreak.effective(user.getCurrentStreak(), user.getLastCompletedDate(), today),
                user.getLongestStreak(),
                today.equals(user.getLastCompletedDate()));

        return new HomeResponse(user.getNickname(), new Daily(today, status, solved, total), streak,
                reader.inProgressGameId(userId).orElse(null), user.getBestStreakWin());
    }

    /**
     * 오늘을 포함한 최근 28일. 기록이 없는 날도 빠짐없이 넣는다.
     */
    public DailyCalendarResponse getDailyCalendar(Long userId) {
        findUser(userId);
        LocalDate today = KstDateTime.today();
        LocalDate from = today.minusDays(CALENDAR_DAYS - 1);
        Map<LocalDate, DailyDay> records = reader.dailyDays(userId, from, today);

        List<Day> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            DailyDay record = records.get(date);
            days.add(record == null
                    ? new Day(date, false, 0)
                    : new Day(date, record.completed(), record.correct()));
        }
        return new DailyCalendarResponse(days);
    }

    /**
     * 점수 추이는 일반 판과 데일리만, 계열별 정답률은 처음 3회와 최근 3회의 처음 푼 답으로 계산한다.
     */
    public GrowthResponse getGrowth(Long userId) {
        findUser(userId);
        List<Session> sessions = reader.completedSessions(userId);

        List<Score> scoreTrend = sessions.stream()
                .filter(session -> !session.weaknessRetry())
                .map(session -> new Score(session.date(), session.type(), session.correctCount(),
                        session.answers().size()))
                .toList();

        List<TypeRecord> typeHistory = reader.typeHistory(userId).stream()
                .map(row -> {
                    WeakType type = WeakType.fromStoredType(row.typeCode());
                    return new TypeRecord(type.name(), type.displayName(), row.recordedDate());
                })
                .toList();

        boolean available = sessions.size() >= COMPARE_SESSIONS;
        List<CategoryAccuracy> categoryAccuracy = available ? categoryAccuracy(sessions) : List.of();
        return new GrowthResponse(available, Math.max(0, COMPARE_SESSIONS - sessions.size()),
                scoreTrend, categoryAccuracy, typeHistory);
    }

    private static List<CategoryAccuracy> categoryAccuracy(List<Session> sessions) {
        // 처음과 최근이 겹치지 않게, 회 수가 적으면 절반씩 비교한다 (3회면 1회 vs 1회, 6회부터 3회 vs 3회)
        int window = Math.min(COMPARE_SESSIONS, sessions.size() / 2);
        List<Session> early = sessions.subList(0, window);
        List<Session> recent = sessions.subList(sessions.size() - window, sessions.size());

        List<CategoryAccuracy> result = new ArrayList<>();
        for (QuestionCategory category : QuestionCategory.values()) {
            result.add(new CategoryAccuracy(category, category.displayName(),
                    accuracy(early, category), accuracy(recent, category)));
        }
        return result;
    }

    private static Double accuracy(List<Session> sessions, QuestionCategory category) {
        int total = 0;
        int correct = 0;
        for (Session session : sessions) {
            for (SessionAnswer answer : session.answers()) {
                if (answer.firstAttempt() && category.name().equals(answer.category())) {
                    total++;
                    if (answer.correct()) {
                        correct++;
                    }
                }
            }
        }
        if (total == 0) {
            return null;
        }
        return Math.round(correct * 100.0 / total) / 100.0;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
    }
}
