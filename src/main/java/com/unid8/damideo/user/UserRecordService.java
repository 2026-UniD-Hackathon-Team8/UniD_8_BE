package com.unid8.damideo.user;

import com.unid8.damideo.answer.FirstAttemptReader;
import com.unid8.damideo.answer.FirstAttemptReader.FirstAttempts;
import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.stats.DailyStreak;
import com.unid8.damideo.stats.WeaknessTypeJudge;
import com.unid8.damideo.stats.WeaknessTypeJudge.TypeJudgement;
import com.unid8.damideo.stats.WeaknessTypeJudge.TypeState;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데일리·판을 마쳤을 때 사용자 기록(연속 데일리, 약점 유형)을 갱신한다.
 * 데일리·판 서비스가 답을 저장한 같은 트랜잭션 안에서 부른다.
 */
@Service
@RequiredArgsConstructor
public class UserRecordService {

    private static final List<String> CATEGORY_ORDER =
            Arrays.stream(QuestionCategory.values()).map(Enum::name).toList();

    private final UserRepository userRepository;
    private final FirstAttemptReader firstAttemptReader;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void completeDaily(Long userId, LocalDate dailyDate) {
        User user = findUser(userId);
        DailyStreak.Record record = DailyStreak.complete(
                user.getCurrentStreak(), user.getLongestStreak(), user.getLastCompletedDate(), dailyDate);
        user.updateDailyStreak(record.currentStreak(), record.longestStreak(), record.lastCompletedDate());
        judgeType(user, dailyDate);
    }

    @Transactional
    public void completeGame(Long userId, Long gameSessionId) {
        judgeType(findUser(userId), KstDateTime.today());
    }

    private void judgeType(User user, LocalDate today) {
        FirstAttempts attempts = firstAttemptReader.read(user.getId());
        TypeState state = new TypeState(user.getCurrentType(), user.getCandidateType(), user.getCandidateSince());
        TypeJudgement judgement = WeaknessTypeJudge.judge(CATEGORY_ORDER, attempts.byCategory(), state, today);
        if (!judgement.opened()) {
            return;
        }

        user.updateType(judgement.currentType(), judgement.candidateType(), judgement.candidateSince());
        if (judgement.changed()) {
            jdbcTemplate.update(
                    "INSERT INTO user_type_history (user_id, type_code, recorded_date) VALUES (?, ?, ?)",
                    user.getId(), judgement.currentType(), Date.valueOf(today));
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
    }
}
