package com.unid8.damideo.game;

import com.unid8.damideo.question.AnswerPos;
import com.unid8.damideo.question.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "game_session_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_pos", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private AnswerPos answerPos;

    public GameSessionQuestion(GameSession gameSession, Question question, int seq, AnswerPos answerPos) {
        this.gameSession = gameSession;
        this.question = question;
        this.seq = seq;
        this.answerPos = answerPos;
    }
}
