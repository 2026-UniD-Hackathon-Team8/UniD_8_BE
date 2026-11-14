package com.unid8.damideo.daily;

import com.unid8.damideo.question.AnswerPos;
import java.time.LocalDate;
import java.util.Map;

/**
 * 데일리 답안 저장·조회 및 완료 통지를 위한 포트.
 * 추후 팀원의 AnswerService / UserRecordService 구현체와 바로 연결할 수 있다.
 */
public interface DailyAnswerPort {

    Map<Long, SubmittedDailyAnswer> findAnswers(Long userId, LocalDate dailyDate);

    SubmittedDailyAnswer recordAnswer(Long userId,
                                      Long questionId,
                                      LocalDate dailyDate,
                                      AnswerPos selectedPos,
                                      boolean correct);

    void onDailyCompleted(Long userId, LocalDate dailyDate);

    record SubmittedDailyAnswer(Long questionId, AnswerPos selectedPos, boolean correct) {
    }
}
