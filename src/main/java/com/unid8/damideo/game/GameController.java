package com.unid8.damideo.game;

import com.unid8.damideo.common.auth.CurrentUser;
import com.unid8.damideo.common.response.ApiResponse;
import com.unid8.damideo.game.dto.GameAnswerRequest;
import com.unid8.damideo.game.dto.GameResultResponse;
import com.unid8.damideo.game.dto.GameSessionResponse;
import com.unid8.damideo.game.dto.StartGameRequest;
import com.unid8.damideo.question.dto.AnswerRevealResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Game", description = "실전 2지선다 판 API")
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "판 시작", description = "일반(NORMAL) 또는 약점 재도전(WEAKNESS_RETRY) 판을 생성합니다. 기존 진행 중인 판은 중단(ABANDONED) 처리됩니다.")
    @PostMapping
    public ApiResponse<GameSessionResponse> startGame(@CurrentUser Long userId,
                                                      @RequestBody(required = false) StartGameRequest request) {
        return ApiResponse.ok("GAME_STARTED", "실전 판을 시작했습니다.", gameService.startGame(userId, request));
    }

    @Operation(summary = "진행 중인 판 이어 하기", description = "현재 진행 중(IN_PROGRESS)인 판의 문제 목록과 진행 상태를 조회합니다.")
    @GetMapping("/current")
    public ApiResponse<GameSessionResponse> getCurrentGame(@CurrentUser Long userId) {
        return ApiResponse.ok("GAME_FETCHED", "진행 중인 판을 조회했습니다.", gameService.getCurrentGame(userId));
    }

    @Operation(summary = "판 답 제출", description = "판 문제 1개에 답을 제출하고 정답·해설을 반환합니다. 마지막 문제 제출 시 판이 완료됩니다.")
    @PostMapping("/{gameSessionId}/answers")
    public ApiResponse<AnswerRevealResponse> submitAnswer(@CurrentUser Long userId,
                                                          @PathVariable Long gameSessionId,
                                                          @Valid @RequestBody GameAnswerRequest request) {
        return ApiResponse.ok("GAME_ANSWER_SUBMITTED", "판 답안을 제출했습니다.",
                gameService.submitGameAnswer(userId, gameSessionId, request));
    }

    @Operation(summary = "판 결과 조회", description = "완료된(COMPLETED) 판의 점수와 계열별 오답 수를 조회합니다.")
    @GetMapping("/{gameSessionId}/result")
    public ApiResponse<GameResultResponse> getGameResult(@CurrentUser Long userId,
                                                         @PathVariable Long gameSessionId) {
        return ApiResponse.ok("GAME_RESULT_FETCHED", "판 결과를 조회했습니다.",
                gameService.getGameResult(userId, gameSessionId));
    }
}
