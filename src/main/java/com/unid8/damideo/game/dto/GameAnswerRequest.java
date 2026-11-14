package com.unid8.damideo.game.dto;

import com.unid8.damideo.question.AnswerPos;
import jakarta.validation.constraints.NotNull;

public record GameAnswerRequest(
        @NotNull(message = "문제 ID는 필수입니다.")
        Long questionId,

        @NotNull(message = "선택한 답(A/B)은 필수입니다.")
        AnswerPos selectedPos
) {
}
