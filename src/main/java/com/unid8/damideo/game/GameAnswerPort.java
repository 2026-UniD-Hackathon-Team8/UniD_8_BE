package com.unid8.damideo.game;

import com.unid8.damideo.question.AnswerPos;
import java.util.Map;

/**
 * 실전 판 답안 저장·조회 및 판 완료 통지를 위한 포트.
 * 추후 팀원의 AnswerService / UserRecordService 구현체와 바로 연결할 수 있다.
 */
public interface GameAnswerPort {

    Map<Long, SubmittedGameAnswer> findAnswers(Long gameSessionId);

    SubmittedGameAnswer recordAnswer(Long userId,
                                     Long gameSessionId,
                                     Long questionId,
                                     AnswerPos selectedPos,
                                     boolean correct);

    void onGameCompleted(Long userId, Long gameSessionId);

    record SubmittedGameAnswer(Long questionId, AnswerPos selectedPos, boolean correct) {
    }
}
