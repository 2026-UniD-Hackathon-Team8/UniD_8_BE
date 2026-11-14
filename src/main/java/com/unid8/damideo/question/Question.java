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
import java.util.Objects;
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

    @Column(name = "notion_id", length = 30)
    private String notionId;

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

    public static Question create(String notionId,
                                  QuestionCategory category,
                                  String tactic,
                                  QuestionType questionType,
                                  String prompt,
                                  String credibleContent,
                                  String targetContent,
                                  String credibleAiAnswer,
                                  String targetAiAnswer,
                                  String explanation,
                                  String checkQuestion,
                                  String source,
                                  byte difficulty,
                                  boolean dailyEligible,
                                  boolean active) {
        Question question = new Question();
        question.notionId = notionId;
        question.category = category;
        question.tactic = tactic;
        question.questionType = questionType;
        question.prompt = prompt;
        question.credibleContent = credibleContent;
        question.targetContent = targetContent;
        question.credibleAiAnswer = credibleAiAnswer;
        question.targetAiAnswer = targetAiAnswer;
        question.explanation = explanation;
        question.checkQuestion = checkQuestion;
        question.source = source;
        question.difficulty = difficulty;
        question.dailyEligible = dailyEligible;
        question.active = active;
        return question;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean hasSameContent(Question other) {
        return Objects.equals(notionId, other.notionId)
                && category == other.category
                && Objects.equals(tactic, other.tactic)
                && questionType == other.questionType
                && Objects.equals(prompt, other.prompt)
                && Objects.equals(credibleContent, other.credibleContent)
                && Objects.equals(targetContent, other.targetContent)
                && Objects.equals(credibleAiAnswer, other.credibleAiAnswer)
                && Objects.equals(targetAiAnswer, other.targetAiAnswer)
                && Objects.equals(explanation, other.explanation)
                && Objects.equals(checkQuestion, other.checkQuestion)
                && Objects.equals(source, other.source)
                && difficulty == other.difficulty
                && dailyEligible == other.dailyEligible
                && active == other.active;
    }
}
