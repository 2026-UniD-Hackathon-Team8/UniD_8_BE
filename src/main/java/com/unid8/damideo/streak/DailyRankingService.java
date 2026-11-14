package com.unid8.damideo.streak;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.stats.DailyStreak;
import com.unid8.damideo.streak.dto.DailyRankingResponse;
import com.unid8.damideo.streak.dto.DailyRankingResponse.MyRank;
import com.unid8.damideo.streak.dto.DailyRankingResponse.RankItem;
import com.unid8.damideo.user.User;
import com.unid8.damideo.user.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연속 데일리 랭킹. 현재 연속 일수는 홈·프로필과 같은 규칙으로 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyRankingService {

    private final UserRepository userRepository;

    public DailyRankingResponse getRanking(Long userId, int size) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
        LocalDate today = KstDateTime.today();

        List<User> active = userRepository.findAllByLastCompletedDateGreaterThanEqual(today.minusDays(1)).stream()
                .filter(user -> streakOf(user, today) > 0)
                .sorted(Comparator.comparingInt((User user) -> streakOf(user, today)).reversed()
                        .thenComparing(User::getLongestStreak, Comparator.reverseOrder()))
                .toList();

        List<RankItem> ranked = new ArrayList<>();
        Integer myRank = null;
        int rank = 0;
        int previous = -1;
        for (int i = 0; i < active.size(); i++) {
            User user = active.get(i);
            int streak = streakOf(user, today);
            if (streak != previous) {
                rank = i + 1;
                previous = streak;
            }
            if (user.getId().equals(userId)) {
                myRank = rank;
            }
            ranked.add(new RankItem(rank, user.getNickname(), streak, user.getLongestStreak()));
        }

        return new DailyRankingResponse(
                ranked.stream().limit(size).toList(),
                new MyRank(myRank, me.getNickname(), streakOf(me, today), me.getLongestStreak()));
    }

    private static int streakOf(User user, LocalDate today) {
        return DailyStreak.effective(user.getCurrentStreak(), user.getLastCompletedDate(), today);
    }
}
