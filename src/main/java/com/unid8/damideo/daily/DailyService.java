package com.unid8.damideo.daily;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.daily.DailyAnswerPort.SubmittedDailyAnswer;
import com.unid8.damideo.daily.dto.DailyAnswerRequest;
import com.unid8.damideo.daily.dto.DailyResponse;
import com.unid8.damideo.daily.dto.DailyResponse.DailyQuestionItem;
import com.unid8.damideo.daily.dto.DailyResultResponse;
import com.unid8.damideo.daily.dto.DailyResultResponse.DailyResultItem;
import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.dto.AnswerRevealResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyService {

    private final DailySetRepository dailySetRepository;
    private final DailySetQuestionRepository dailySetQuestionRepository;
    private final DailySetCreator dailySetCreator;
    private final DailyAnswerPort dailyAnswerPort;

    public DailyService(DailySetRepository dailySetRepository,
                        DailySetQuestionRepository dailySetQuestionRepository,
                        DailySetCreator dailySetCreator,
                        DailyAnswerPort dailyAnswerPort) {
        this.dailySetRepository = dailySetRepository;
        this.dailySetQuestionRepository = dailySetQuestionRepository;
        this.dailySetCreator = dailySetCreator;
        this.dailyAnswerPort = dailyAnswerPort;
    }

    public DailyResponse getTodayDaily(Long userId) {
        LocalDate today = KstDateTime.today();
        DailySet dailySet = getOrCreateTodaySet(today);
        List<DailySetQuestion> setQuestions = dailySetQuestionRepository.findAllWithQuestionByDailySetId(dailySet.getId());
        Map<Long, SubmittedDailyAnswer> answers = dailyAnswerPort.findAnswers(userId, today);

        List<DailyQuestionItem> items = setQuestions.stream()
                .map(dsq -> {
                    SubmittedDailyAnswer ans = answers.get(dsq.getQuestion().getId());
                    AnswerPos selected = ans != null ? ans.selectedPos() : null;
                    return DailyQuestionItem.of(dsq.getQuestion(), dsq.getSeq(), dsq.getAnswerPos(), selected);
                })
                .toList();

        int solvedCount = (int) items.stream().filter(DailyQuestionItem::answered).count();
        boolean completed = !items.isEmpty() && solvedCount == items.size();

        return new DailyResponse(today, completed, solvedCount, items.size(), items);
    }

    @Transactional
    public AnswerRevealResponse submitDailyAnswer(Long userId, DailyAnswerRequest request) {
        LocalDate today = KstDateTime.today();
        DailySet dailySet = getOrCreateTodaySet(today);
        List<DailySetQuestion> setQuestions = dailySetQuestionRepository.findAllWithQuestionByDailySetId(dailySet.getId());

        DailySetQuestion target = setQuestions.stream()
                .filter(dsq -> dsq.getQuestion().getId().equals(request.questionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(DailyErrorCode.DAILY_QUESTION_NOT_FOUND));

        Map<Long, SubmittedDailyAnswer> beforeAnswers = dailyAnswerPort.findAnswers(userId, today);
        boolean wasCompleted = !setQuestions.isEmpty() && beforeAnswers.size() >= setQuestions.size();

        boolean isCorrect = request.selectedPos() == target.getAnswerPos();
        SubmittedDailyAnswer saved = dailyAnswerPort.recordAnswer(
                userId,
                target.getQuestion().getId(),
                today,
                request.selectedPos(),
                isCorrect
        );

        Map<Long, SubmittedDailyAnswer> afterAnswers = dailyAnswerPort.findAnswers(userId, today);
        boolean isCompleted = !setQuestions.isEmpty() && afterAnswers.size() >= setQuestions.size();
        if (!wasCompleted && isCompleted) {
            dailyAnswerPort.onDailyCompleted(userId, today);
        }

        return AnswerRevealResponse.of(
                target.getQuestion(),
                saved.selectedPos(),
                target.getAnswerPos(),
                isCompleted
        );
    }

    @Transactional(readOnly = true)
    public DailyResultResponse getTodayDailyResult(Long userId) {
        LocalDate today = KstDateTime.today();
        DailySet dailySet = dailySetRepository.findByDailyDate(today)
                .orElseThrow(() -> new BusinessException(DailyErrorCode.DAILY_NOT_COMPLETED));
        List<DailySetQuestion> setQuestions = dailySetQuestionRepository.findAllWithQuestionByDailySetId(dailySet.getId());
        Map<Long, SubmittedDailyAnswer> answers = dailyAnswerPort.findAnswers(userId, today);

        if (setQuestions.isEmpty() || answers.size() < setQuestions.size()) {
            throw new BusinessException(DailyErrorCode.DAILY_NOT_COMPLETED);
        }

        List<DailyResultItem> items = setQuestions.stream()
                .map(dsq -> {
                    SubmittedDailyAnswer ans = answers.get(dsq.getQuestion().getId());
                    if (ans == null) {
                        throw new BusinessException(DailyErrorCode.DAILY_NOT_COMPLETED);
                    }
                    return DailyResultItem.of(dsq.getQuestion(), dsq.getSeq(), ans.selectedPos(), dsq.getAnswerPos());
                })
                .toList();

        int correctCount = (int) items.stream().filter(DailyResultItem::correct).count();
        return new DailyResultResponse(today, items.size(), correctCount, items);
    }

    private DailySet getOrCreateTodaySet(LocalDate today) {
        return dailySetRepository.findByDailyDate(today)
                .orElseGet(() -> {
                    try {
                        return dailySetCreator.create(today);
                    } catch (DataIntegrityViolationException ex) {
                        return dailySetRepository.findByDailyDate(today)
                                .orElseThrow(() -> ex);
                    }
                });
    }
}
