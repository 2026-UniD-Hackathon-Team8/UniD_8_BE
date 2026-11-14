package com.unid8.damideo.user;

import com.unid8.damideo.common.auth.CurrentUser;
import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.user.dto.DailyCalendarResponse;
import com.unid8.damideo.user.dto.GrowthResponse;
import com.unid8.damideo.user.dto.HomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyRecordController {

    private final MyRecordService myRecordService;

    @Operation(summary = "홈 조회", description = "오늘 데일리 상태, 연속 데일리, 진행 중인 판, 최고 연승")
    @GetMapping("/api/home")
    public ApiResponse<HomeResponse> home(@CurrentUser Long userId) {
        return ApiResponse.ok("HOME_SUCCESS", "홈 정보를 조회했습니다.", myRecordService.getHome(userId));
    }

    @Operation(summary = "데일리 달력", description = "오늘을 포함한 최근 28일")
    @GetMapping("/api/users/me/daily-calendar")
    public ApiResponse<DailyCalendarResponse> dailyCalendar(@CurrentUser Long userId) {
        return ApiResponse.ok("DAILY_CALENDAR_SUCCESS", "데일리 달력을 조회했습니다.",
                myRecordService.getDailyCalendar(userId));
    }

    @Operation(summary = "나의 성장", description = "점수 추이, 처음 3회 vs 최근 3회 계열별 정답률, 지나온 유형")
    @GetMapping("/api/users/me/growth")
    public ApiResponse<GrowthResponse> growth(@CurrentUser Long userId) {
        return ApiResponse.ok("GROWTH_SUCCESS", "성장 기록을 조회했습니다.", myRecordService.getGrowth(userId));
    }
}
