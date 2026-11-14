package com.unid8.damideo.streak;

import com.unid8.damideo.question.AnswerPos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "streak_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreakChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StreakStatus status;

    @Column(name = "win_count", nullable = false)
    private int winCount;

    @Column(name = "current_question_id")
    private Long currentQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_answer_pos", length = 1, columnDefinition = "CHAR(1)")
    private AnswerPos currentAnswerPos;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    private StreakChallenge(Long userId, Long questionId, AnswerPos answerPos, LocalDateTime now) {
        this.userId = userId;
        this.status = StreakStatus.IN_PROGRESS;
        this.currentQuestionId = questionId;
        this.currentAnswerPos = answerPos;
        this.createdAt = now;
    }

    public static StreakChallenge start(Long userId, Long questionId, AnswerPos answerPos, LocalDateTime now) {
        return new StreakChallenge(userId, questionId, answerPos, now);
    }

    public boolean isEnded() {
        return status == StreakStatus.ENDED;
    }

    public void win() {
        this.winCount++;
    }

    public void nextQuestion(Long questionId, AnswerPos answerPos) {
        this.currentQuestionId = questionId;
        this.currentAnswerPos = answerPos;
    }

    public void end(LocalDateTime now) {
        this.status = StreakStatus.ENDED;
        this.currentQuestionId = null;
        this.currentAnswerPos = null;
        this.endedAt = now;
    }
}
