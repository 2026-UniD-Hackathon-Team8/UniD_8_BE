package com.unid8.damideo.streak;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.game.dto.GameSessionResponse.GameQuestionItem;
import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionPicker;
import com.unid8.damideo.question.QuestionRepository;
import com.unid8.damideo.question.dto.AnswerRevealResponse;
import com.unid8.damideo.stats.StreakRecord;
import com.unid8.damideo.stats.StreakRecord.Best;
import com.unid8.damideo.streak.dto.StreakAnswerRequest;
import com.unid8.damideo.streak.dto.StreakAnswerResponse;
import com.unid8.damideo.streak.dto.StreakStartResponse;
import com.unid8.damideo.user.User;
import com.unid8.damideo.user.UserRecordService;
import com.unid8.damideo.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연승 챌린지. 문제를 하나씩 내고, 틀리면 끝난다. 사용자당 진행 중인 도전은 하나.
 */
@Service
@RequiredArgsConstructor
public class StreakChallengeService {

    private final StreakChallengeRepository challengeRepository;
    private final StreakAnswerWriter answerWriter;
    private final QuestionPicker questionPicker;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final UserRecordService userRecordService;
    private final Random random = new Random();

    /**
     * 진행 중인 도전이 있으면 그때까지의 연승으로 끝내고 새 도전을 시작한다.
     */
    @Transactional
    public StreakStartResponse start(Long userId) {
        User user = findUser(userId);
        LocalDateTime now = KstDateTime.nowUtc();
        for (StreakChallenge running : challengeRepository.findAllByUserIdAndStatus(userId, StreakStatus.IN_PROGRESS)) {
            running.end(now);
        }

        Question question = questionPicker.pickForStreak(userId, Set.of())
                .orElseThrow(() -> new BusinessException(StreakErrorCode.NO_QUESTION_AVAILABLE));
        AnswerPos answerPos = randomPos();
        StreakChallenge challenge = challengeRepository.save(
                StreakChallenge.start(userId, question.getId(), answerPos, now));

        return new StreakStartResponse(challenge.getId(), user.getBestStreakWin(),
                GameQuestionItem.of(question, 1, answerPos, null));
    }

    @Transactional
    public StreakAnswerResponse submit(Long userId, Long challengeId, StreakAnswerRequest request) {
        StreakChallenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(StreakErrorCode.CHALLENGE_NOT_FOUND));
        if (challenge.isEnded()) {
            throw new BusinessException(StreakErrorCode.CHALLENGE_ENDED);
        }
        if (!request.questionId().equals(challenge.getCurrentQuestionId())) {
            throw new BusinessException(StreakErrorCode.NOT_CURRENT_QUESTION);
        }

        Question question = questionRepository.findById(challenge.getCurrentQuestionId())
                .orElseThrow(() -> new BusinessException(StreakErrorCode.CHALLENGE_NOT_FOUND));
        AnswerPos answerPos = challenge.getCurrentAnswerPos();
        boolean correct = request.selectedPos() == answerPos;
        answerWriter.record(userId, challengeId, question.getId(), request.selectedPos(), correct);

        User user = findUser(userId);
        LocalDateTime now = KstDateTime.nowUtc();
        if (!correct) {
            return finish(user, challenge, question, request.selectedPos(), answerPos, false, "WRONG", now);
        }

        challenge.win();
        boolean newRecord = updateBest(user, challenge.getWinCount(), now);
        Optional<Question> next = questionPicker.pickForStreak(userId, answerWriter.askedQuestionIds(challengeId));
        if (next.isEmpty()) {
            return finish(user, challenge, question, request.selectedPos(), answerPos, newRecord, "COMPLETED", now);
        }

        AnswerPos nextPos = randomPos();
        challenge.nextQuestion(next.get().getId(), nextPos);
        return new StreakAnswerResponse(
                AnswerRevealResponse.of(question, request.selectedPos(), answerPos, false),
                challenge.getWinCount(), user.getBestStreakWin(), newRecord, false, null,
                GameQuestionItem.of(next.get(), challenge.getWinCount() + 1, nextPos, null));
    }

    private StreakAnswerResponse finish(User user, StreakChallenge challenge, Question question,
                                        AnswerPos selected, AnswerPos answerPos, boolean newRecord,
                                        String reason, LocalDateTime now) {
        challenge.end(now);
        userRecordService.completeGame(user.getId(), null);
        return new StreakAnswerResponse(
                AnswerRevealResponse.of(question, selected, answerPos, true),
                challenge.getWinCount(), user.getBestStreakWin(), newRecord, true, reason, null);
    }

    /**
     * 맞힐 때마다 바로 최고 기록과 비교한다. 중간에 나가도 기록이 남는다.
     */
    private boolean updateBest(User user, int wins, LocalDateTime now) {
        Best current = new Best(user.getBestStreakWin(), user.getBestStreakWinAt());
        Best updated = StreakRecord.updateBest(current, wins, now);
        if (updated.equals(current)) {
            return false;
        }
        user.updateBestStreak(updated.wins(), updated.achievedAt());
        return true;
    }

    private AnswerPos randomPos() {
        return random.nextBoolean() ? AnswerPos.A : AnswerPos.B;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
    }
}
