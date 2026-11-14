package com.unid8.damideo.user;

import com.unid8.damideo.answer.FirstAttemptReader;
import com.unid8.damideo.answer.FirstAttemptReader.FirstAttempts;
import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.stats.DailyStreak;
import com.unid8.damideo.stats.WeakType;
import com.unid8.damideo.stats.WeaknessTypeJudge;
import com.unid8.damideo.user.dto.ProfileResponse;
import com.unid8.damideo.user.dto.ProfileResponse.Progress;
import com.unid8.damideo.user.dto.ProfileResponse.Streak;
import com.unid8.damideo.user.dto.ProfileResponse.WeakTypeView;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    static final int REQUIRED_ANSWERS = WeaknessTypeJudge.MIN_TOTAL;
    private static final List<String> CATEGORY_ORDER =
            Arrays.stream(QuestionCategory.values()).map(Enum::name).toList();

    private final UserRepository userRepository;
    private final FirstAttemptReader firstAttemptReader;

    /**
     * 유형은 저장된 값을 읽는다. 유형 계산은 데일리·판을 마칠 때 한다.
     */
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
        LocalDate today = KstDateTime.today();

        Streak streak = new Streak(
                DailyStreak.effective(user.getCurrentStreak(), user.getLastCompletedDate(), today),
                user.getLongestStreak(),
                today.equals(user.getLastCompletedDate()));

        FirstAttempts attempts = firstAttemptReader.read(userId);
        return new ProfileResponse(user.getNickname(), streak, user.getBestStreakWin(),
                weakType(user.getCurrentType(), attempts));
    }

    private WeakTypeView weakType(String storedType, FirstAttempts attempts) {
        Progress progress = new Progress(attempts.total(), REQUIRED_ANSWERS);
        if (storedType == null) {
            return new WeakTypeView("ANALYZING", null, null, null, null, null, progress);
        }

        WeakType type = WeakType.fromStoredType(storedType);
        return new WeakTypeView("DECIDED", type.name(), type.displayName(), type.weakCategory(),
                strongest(attempts), type.checkQuestion(), progress);
    }

    /**
     * 정답률이 가장 높은 계열. 같으면 계열 순서가 앞선 쪽.
     */
    private static String strongest(FirstAttempts attempts) {
        Map<String, Double> accuracies = WeaknessTypeJudge.accuracies(CATEGORY_ORDER, attempts.byCategory());
        String strongest = null;
        double highest = -1;
        for (String category : CATEGORY_ORDER) {
            Double accuracy = accuracies.get(category);
            if (accuracy != null && accuracy > highest) {
                highest = accuracy;
                strongest = category;
            }
        }
        return strongest;
    }
}
