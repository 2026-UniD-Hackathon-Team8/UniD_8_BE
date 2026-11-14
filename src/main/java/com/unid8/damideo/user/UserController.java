package com.unid8.damideo.user;

import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.user.dto.CreateUserRequest;
import com.unid8.damideo.user.dto.CreateUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "게스트 계정 생성", description = "X-Device-Id 헤더 없이 호출한다. 같은 기기면 기존 계정을 돌려준다.")
    @SecurityRequirements
    @PostMapping
    public ApiResponse<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userService.createGuest(request.deviceId(), request.nickname());
        if (response.created()) {
            return ApiResponse.ok("USER_CREATED", "계정을 만들었습니다.", response);
        }
        return ApiResponse.ok("USER_FOUND", "기존 계정을 불러왔습니다.", response);
    }
}
