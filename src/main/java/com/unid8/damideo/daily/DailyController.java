package com.unid8.damideo.daily;

import com.unid8.damideo.common.auth.CurrentUser;
import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.daily.dto.DailyAnswerRequest;
import com.unid8.damideo.daily.dto.DailyResponse;
import com.unid8.damideo.daily.dto.DailyResultResponse;
import com.unid8.damideo.question.dto.AnswerRevealResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Daily", description = "오늘의 데일리 5문제 API")
@RestController
@RequestMapping("/api/daily")
public class DailyController {

    private final DailyService dailyService;

    public DailyController(DailyService dailyService) {
        this.dailyService = dailyService;
    }

    @Operation(summary = "오늘의 데일리 문제 조회", description = "오늘(KST) 데일리 5문제와 내 진행 상태를 조회합니다.")
    @GetMapping
    public ApiResponse<DailyResponse> getTodayDaily(@CurrentUser Long userId) {
        return ApiResponse.ok("DAILY_FETCHED", "오늘의 데일리 문제를 조회했습니다.", dailyService.getTodayDaily(userId));
    }

    @Operation(summary = "데일리 답 제출", description = "데일리 문제 1개에 답을 제출하고 정답·해설을 반환합니다.")
    @PostMapping("/answers")
    public ApiResponse<AnswerRevealResponse> submitAnswer(@CurrentUser Long userId,
                                                          @Valid @RequestBody DailyAnswerRequest request) {
        return ApiResponse.ok("DAILY_ANSWER_SUBMITTED", "데일리 답안을 제출했습니다.",
                dailyService.submitDailyAnswer(userId, request));
    }

    @Operation(summary = "오늘의 데일리 결과 조회", description = "오늘 데일리 5문제를 모두 푼 뒤 결과를 조회합니다.")
    @GetMapping("/result")
    public ApiResponse<DailyResultResponse> getTodayResult(@CurrentUser Long userId) {
        return ApiResponse.ok("DAILY_RESULT_FETCHED", "오늘의 데일리 결과를 조회했습니다.",
                dailyService.getTodayDailyResult(userId));
    }
}
