package com.unid8.damideo.question;

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

@Getter
@Entity
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionCategory category;

    @Column(nullable = false, length = 50)
    private String tactic;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "credible_content", nullable = false, columnDefinition = "TEXT")
    private String credibleContent;

    @Column(name = "target_content", nullable = false, columnDefinition = "TEXT")
    private String targetContent;

    @Column(name = "credible_ai_answer", columnDefinition = "TEXT")
    private String credibleAiAnswer;

    @Column(name = "target_ai_answer", columnDefinition = "TEXT")
    private String targetAiAnswer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "check_question", nullable = false, columnDefinition = "TEXT")
    private String checkQuestion;

    @Column(nullable = false, length = 1000)
    private String source;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private byte difficulty;

    @Column(name = "is_daily_eligible", nullable = false)
    private boolean dailyEligible;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime updatedAt;

    public String optionA(AnswerPos answerPos) {
        return answerPos == AnswerPos.A ? targetContent : credibleContent;
    }

    public String optionB(AnswerPos answerPos) {
        return answerPos == AnswerPos.B ? targetContent : credibleContent;
    }
}
