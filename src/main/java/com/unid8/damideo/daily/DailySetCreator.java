package com.unid8.damideo.daily;

import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.AnswerPositions;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionPicker;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오늘의 데일리 세트를 별도 트랜잭션으로 생성한다.
 * 동시 요청으로 unique 제약 충돌이 나면 호출 측에서 기존 세트를 다시 조회할 수 있게 한다.
 */
@Component
class DailySetCreator {

    private final DailySetRepository dailySetRepository;
    private final DailySetQuestionRepository dailySetQuestionRepository;
    private final QuestionPicker questionPicker;

    DailySetCreator(DailySetRepository dailySetRepository,
                    DailySetQuestionRepository dailySetQuestionRepository,
                    QuestionPicker questionPicker) {
        this.dailySetRepository = dailySetRepository;
        this.dailySetQuestionRepository = dailySetQuestionRepository;
        this.questionPicker = questionPicker;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DailySet create(LocalDate dailyDate) {
        DailySet savedSet = dailySetRepository.saveAndFlush(new DailySet(dailyDate));
        List<Question> picked = questionPicker.pickForDaily(dailyDate);
        Random posRandom = new Random(dailyDate.toEpochDay() * 31L + 7L);
        List<AnswerPos> positions = AnswerPositions.balanced(picked.size(), posRandom);

        for (int i = 0; i < picked.size(); i++) {
            dailySetQuestionRepository.save(
                    new DailySetQuestion(savedSet, picked.get(i), (byte) (i + 1), positions.get(i)));
        }
        return savedSet;
    }
}
