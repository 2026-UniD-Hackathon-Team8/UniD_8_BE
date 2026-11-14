package com.unid8.damideo.streak.dto;

import com.unid8.damideo.question.AnswerPos;
import jakarta.validation.constraints.NotNull;

public record StreakAnswerRequest(
        @NotNull(message = "문제 ID는 필수입니다.")
        Long questionId,

        @NotNull(message = "선택한 답(A/B)은 필수입니다.")
        AnswerPos selectedPos
) {
}
