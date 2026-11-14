package com.unid8.damideo.streak;

import com.unid8.damideo.common.auth.CurrentUser;
import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.streak.dto.RankingResponse;
import com.unid8.damideo.streak.dto.StreakAnswerRequest;
import com.unid8.damideo.streak.dto.StreakAnswerResponse;
import com.unid8.damideo.streak.dto.StreakStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StreakController {

    private final StreakChallengeService challengeService;
    private final RankingService rankingService;

    @Operation(summary = "연승 챌린지 시작", description = "진행 중인 도전이 있으면 끝내고 새로 시작한다. 첫 문제가 함께 온다.")
    @PostMapping("/api/streak-challenges")
    public ApiResponse<StreakStartResponse> start(@CurrentUser Long userId) {
        return ApiResponse.ok("STREAK_CHALLENGE_STARTED", "연승 챌린지를 시작했습니다.", challengeService.start(userId));
    }

    @Operation(summary = "연승 챌린지 답 제출", description = "맞히면 다음 문제가 오고, 틀리면 끝난다.")
    @PostMapping("/api/streak-challenges/{challengeId}/answers")
    public ApiResponse<StreakAnswerResponse> submit(@CurrentUser Long userId,
                                                    @PathVariable Long challengeId,
                                                    @Valid @RequestBody StreakAnswerRequest request) {
        return ApiResponse.ok("ANSWER_SUBMITTED", "답을 제출했습니다.",
                challengeService.submit(userId, challengeId, request));
    }

    @Operation(summary = "연승 랭킹", description = "최고 연승 내림차순, 같으면 먼저 달성한 순. 동점은 같은 순위")
    @GetMapping("/api/rankings")
    public ApiResponse<RankingResponse> ranking(@CurrentUser Long userId,
                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("RANKING_SUCCESS", "랭킹을 조회했습니다.",
                rankingService.getRanking(userId, Math.max(1, Math.min(size, 100))));
    }
}
