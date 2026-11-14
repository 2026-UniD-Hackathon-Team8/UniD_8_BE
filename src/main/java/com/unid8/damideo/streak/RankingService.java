package com.unid8.damideo.streak;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.stats.StreakRecord;
import com.unid8.damideo.stats.StreakRecord.Best;
import com.unid8.damideo.stats.StreakRecord.Entry;
import com.unid8.damideo.stats.StreakRecord.Ranked;
import com.unid8.damideo.streak.dto.RankingResponse;
import com.unid8.damideo.streak.dto.RankingResponse.MyRank;
import com.unid8.damideo.streak.dto.RankingResponse.RankItem;
import com.unid8.damideo.user.User;
import com.unid8.damideo.user.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연승 랭킹. 사용자가 적어서 요청마다 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final UserRepository userRepository;

    public RankingResponse getRanking(Long userId, int size) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        List<Entry<User>> entries = userRepository.findAllByBestStreakWinGreaterThan(0).stream()
                .map(user -> new Entry<>(user, new Best(user.getBestStreakWin(), user.getBestStreakWinAt())))
                .toList();
        List<Ranked<User>> ranked = StreakRecord.rank(entries);

        List<RankItem> top = ranked.stream()
                .limit(size)
                .map(r -> new RankItem(r.rank(), r.owner().getNickname(), r.best().wins(), toKst(r.best().achievedAt())))
                .toList();
        Integer myRank = ranked.stream()
                .filter(r -> r.owner().getId().equals(userId))
                .map(Ranked::rank)
                .findFirst()
                .orElse(null);

        return new RankingResponse(top, new MyRank(myRank, me.getNickname(), me.getBestStreakWin()));
    }

    private static OffsetDateTime toKst(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(KstDateTime.KST).toOffsetDateTime();
    }
}
