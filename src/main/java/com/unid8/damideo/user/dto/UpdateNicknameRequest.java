package com.unid8.damideo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(

        @NotBlank(message = CreateUserRequest.NICKNAME_MESSAGE)
        @Size(min = 2, max = 10, message = CreateUserRequest.NICKNAME_MESSAGE)
        String nickname
) {
}
