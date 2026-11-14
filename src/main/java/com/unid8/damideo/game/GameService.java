package com.unid8.damideo.game;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.game.GameAnswerPort.SubmittedGameAnswer;
import com.unid8.damideo.game.dto.GameAnswerRequest;
import com.unid8.damideo.game.dto.GameResultResponse;
import com.unid8.damideo.game.dto.GameResultResponse.CategoryWrongCount;
import com.unid8.damideo.game.dto.GameResultResponse.GameResultItem;
import com.unid8.damideo.game.dto.GameSessionResponse;
import com.unid8.damideo.game.dto.GameSessionResponse.GameQuestionItem;
import com.unid8.damideo.game.dto.StartGameRequest;
import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.AnswerRevealResponse;
import com.unid8.damideo.question.Question;
import com.unid8.damideo.question.QuestionCategory;
import com.unid8.damideo.question.QuestionPicker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private final GameSessionRepository gameSessionRepository;
    private final GameSessionQuestionRepository gameSessionQuestionRepository;
    private final QuestionPicker questionPicker;
    private final GameAnswerPort gameAnswerPort;
    private final int defaultQuestionCount;

    public GameService(GameSessionRepository gameSessionRepository,
                       GameSessionQuestionRepository gameSessionQuestionRepository,
                       QuestionPicker questionPicker,
                       GameAnswerPort gameAnswerPort,
                       @Value("${damideo.game.default-question-count:10}") int defaultQuestionCount) {
        this.gameSessionRepository = gameSessionRepository;
        this.gameSessionQuestionRepository = gameSessionQuestionRepository;
        this.questionPicker = questionPicker;
        this.gameAnswerPort = gameAnswerPort;
        this.defaultQuestionCount = defaultQuestionCount;
    }

    @Transactional
    public GameSessionResponse startGame(Long userId, StartGameRequest request) {
        GameMode mode = request == null ? GameMode.NORMAL : request.resolvedMode();

        List<GameSession> ongoing = gameSessionRepository.findAllByUserIdAndStatus(userId, GameStatus.IN_PROGRESS);
        for (GameSession oldSession : ongoing) {
            oldSession.abandon();
        }

        List<Question> picked = questionPicker.pickForGame(userId, mode, defaultQuestionCount, KstDateTime.today());
        if (picked.isEmpty()) {
            throw new BusinessException(GameErrorCode.NO_ACTIVE_QUESTIONS);
        }

        GameSession session = gameSessionRepository.save(new GameSession(userId, mode, picked.size()));
        Random posRandom = new Random();

        List<GameSessionQuestion> savedQuestions = new ArrayList<>(picked.size());
        for (int i = 0; i < picked.size(); i++) {
            AnswerPos answerPos = posRandom.nextBoolean() ? AnswerPos.A : AnswerPos.B;
            savedQuestions.add(gameSessionQuestionRepository.save(
                    new GameSessionQuestion(session, picked.get(i), i + 1, answerPos)
            ));
        }

        return toSessionResponse(session, savedQuestions, Map.of());
    }

    @Transactional(readOnly = true)
    public GameSessionResponse getCurrentGame(Long userId) {
        GameSession session = gameSessionRepository
                .findFirstByUserIdAndStatusOrderByIdDesc(userId, GameStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException(GameErrorCode.GAME_SESSION_NOT_FOUND));

        List<GameSessionQuestion> sessionQuestions =
                gameSessionQuestionRepository.findAllWithQuestionByGameSessionId(session.getId());
        Map<Long, SubmittedGameAnswer> answers = gameAnswerPort.findAnswers(session.getId());

        return toSessionResponse(session, sessionQuestions, answers);
    }

    @Transactional
    public AnswerRevealResponse submitGameAnswer(Long userId, Long gameSessionId, GameAnswerRequest request) {
        GameSession session = loadOwnedSession(userId, gameSessionId);
        if (session.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BusinessException(GameErrorCode.GAME_NOT_IN_PROGRESS);
        }

        List<GameSessionQuestion> sessionQuestions =
                gameSessionQuestionRepository.findAllWithQuestionByGameSessionId(session.getId());
        GameSessionQuestion target = sessionQuestions.stream()
                .filter(gsq -> gsq.getQuestion().getId().equals(request.questionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(GameErrorCode.GAME_QUESTION_NOT_FOUND));

        boolean isCorrect = request.selectedPos() == target.getAnswerPos();
        SubmittedGameAnswer saved = gameAnswerPort.recordAnswer(
                userId,
                session.getId(),
                target.getQuestion().getId(),
                request.selectedPos(),
                isCorrect
        );

        Map<Long, SubmittedGameAnswer> afterAnswers = gameAnswerPort.findAnswers(session.getId());
        boolean isCompleted = !sessionQuestions.isEmpty() && afterAnswers.size() >= sessionQuestions.size();
        if (isCompleted) {
            session.complete(KstDateTime.nowUtc());
            gameAnswerPort.onGameCompleted(userId, session.getId());
        }

        return AnswerRevealResponse.of(
                target.getQuestion(),
                saved.selectedPos(),
                target.getAnswerPos(),
                isCompleted
        );
    }

    @Transactional(readOnly = true)
    public GameResultResponse getGameResult(Long userId, Long gameSessionId) {
        GameSession session = loadOwnedSession(userId, gameSessionId);
        if (session.getStatus() != GameStatus.COMPLETED) {
            throw new BusinessException(GameErrorCode.GAME_NOT_COMPLETED);
        }

        List<GameSessionQuestion> sessionQuestions =
                gameSessionQuestionRepository.findAllWithQuestionByGameSessionId(session.getId());
        Map<Long, SubmittedGameAnswer> answers = gameAnswerPort.findAnswers(session.getId());

        Map<QuestionCategory, Integer> wrongByCategory = new EnumMap<>(QuestionCategory.class);
        List<GameResultItem> items = new ArrayList<>(sessionQuestions.size());
        int correctCount = 0;

        for (GameSessionQuestion gsq : sessionQuestions) {
            SubmittedGameAnswer ans = answers.get(gsq.getQuestion().getId());
            if (ans == null) {
                throw new BusinessException(GameErrorCode.GAME_NOT_COMPLETED);
            }
            GameResultItem item = GameResultItem.of(
                    gsq.getQuestion(),
                    gsq.getSeq(),
                    ans.selectedPos(),
                    gsq.getAnswerPos()
            );
            items.add(item);
            if (item.correct()) {
                correctCount++;
            } else {
                wrongByCategory.merge(gsq.getQuestion().getCategory(), 1, Integer::sum);
            }
        }

        List<CategoryWrongCount> categoryWrongCounts = wrongByCategory.entrySet().stream()
                .sorted(Map.Entry.<QuestionCategory, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> e.getKey().ordinal()))
                .map(e -> new CategoryWrongCount(e.getKey(), e.getKey().displayName(), e.getValue()))
                .toList();

        return new GameResultResponse(
                session.getId(),
                session.getGameMode(),
                items.size(),
                correctCount,
                categoryWrongCounts,
                items
        );
    }

    private GameSession loadOwnedSession(Long userId, Long gameSessionId) {
        GameSession session = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new BusinessException(GameErrorCode.GAME_SESSION_NOT_FOUND));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(GameErrorCode.GAME_SESSION_FORBIDDEN);
        }
        return session;
    }

    private GameSessionResponse toSessionResponse(GameSession session,
                                                  List<GameSessionQuestion> sessionQuestions,
                                                  Map<Long, SubmittedGameAnswer> answers) {
        List<GameQuestionItem> items = sessionQuestions.stream()
                .map(gsq -> {
                    SubmittedGameAnswer ans = answers.get(gsq.getQuestion().getId());
                    AnswerPos selected = ans != null ? ans.selectedPos() : null;
                    return GameQuestionItem.of(gsq.getQuestion(), gsq.getSeq(), gsq.getAnswerPos(), selected);
                })
                .toList();

        int solvedCount = (int) items.stream().filter(GameQuestionItem::answered).count();
        int nextSeq = items.stream()
                .filter(item -> !item.answered())
                .mapToInt(GameQuestionItem::seq)
                .findFirst()
                .orElse(items.size() + 1);

        return new GameSessionResponse(
                session.getId(),
                session.getGameMode(),
                session.getStatus(),
                solvedCount,
                items.size(),
                nextSeq,
                items
        );
    }
}
