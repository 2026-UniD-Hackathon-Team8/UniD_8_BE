package com.unid8.damideo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = DEVICE_ID_MESSAGE)
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = DEVICE_ID_MESSAGE)
        String deviceId,

        @NotBlank(message = NICKNAME_MESSAGE)
        @Size(min = 2, max = 10, message = NICKNAME_MESSAGE)
        String nickname
) {

    public static final String DEVICE_ID_MESSAGE = "기기 ID 형식이 올바르지 않습니다.";
    public static final String NICKNAME_MESSAGE = "2~10자로 입력해주세요.";
}
