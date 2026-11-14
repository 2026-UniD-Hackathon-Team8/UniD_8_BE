package com.unid8.damideo.user;

import com.unid8.damideo.common.auth.CurrentUser;
import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.user.dto.NicknameResponse;
import com.unid8.damideo.user.dto.ProfileResponse;
import com.unid8.damideo.user.dto.UpdateNicknameRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final ProfileService profileService;

    @Operation(summary = "닉네임 변경")
    @PatchMapping("/nickname")
    public ApiResponse<NicknameResponse> changeNickname(@CurrentUser Long userId,
                                                       @Valid @RequestBody UpdateNicknameRequest request) {
        return ApiResponse.ok("NICKNAME_UPDATED", "닉네임을 바꿨습니다.",
                userService.changeNickname(userId, request.nickname()));
    }

    @Operation(summary = "프로필 조회", description = "닉네임, 연속 데일리, 최고 연승, 약점 유형")
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(@CurrentUser Long userId) {
        return ApiResponse.ok("PROFILE_SUCCESS", "프로필을 조회했습니다.", profileService.getProfile(userId));
    }
}
