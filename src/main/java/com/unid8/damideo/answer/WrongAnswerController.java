package com.unid8.damideo.answer;

import com.unid8.damideo.answer.dto.MemoRequest;
import com.unid8.damideo.answer.dto.MemoResponse;
import com.unid8.damideo.answer.dto.WrongAnswersResponse;
import com.unid8.damideo.common.auth.CurrentUser;
import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.question.QuestionCategory;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/wrong-answers")
@RequiredArgsConstructor
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    @Operation(summary = "오답노트 조회", description = "틀린 답 최신순. 같은 문제는 최신 1건. category로 거를 수 있다.")
    @GetMapping
    public ApiResponse<WrongAnswersResponse> list(@CurrentUser Long userId,
                                                  @RequestParam(required = false) QuestionCategory category) {
        return ApiResponse.ok("WRONG_ANSWERS_SUCCESS", "오답노트를 조회했습니다.",
                wrongAnswerService.getWrongAnswers(userId, category));
    }

    @Operation(summary = "오답 메모 저장", description = "있으면 덮어쓴다.")
    @PutMapping("/{questionId}/memo")
    public ApiResponse<MemoResponse> saveMemo(@CurrentUser Long userId,
                                              @PathVariable Long questionId,
                                              @Valid @RequestBody MemoRequest request) {
        return ApiResponse.ok("MEMO_SAVED", "메모를 저장했습니다.",
                wrongAnswerService.saveMemo(userId, questionId, request.memo()));
    }
}
