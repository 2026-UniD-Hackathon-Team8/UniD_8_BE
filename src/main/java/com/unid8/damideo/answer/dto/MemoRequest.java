package com.unid8.damideo.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemoRequest(
        @NotBlank(message = "메모는 1~500자로 입력해주세요.")
        @Size(max = 500, message = "메모는 1~500자로 입력해주세요.")
        String memo
) {
}
