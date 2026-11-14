package com.unid8.damideo.daily;

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
@Table(name = "daily_set_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySetQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_set_id", nullable = false)
    private DailySet dailySet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private byte seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_pos", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private AnswerPos answerPos;

    public DailySetQuestion(DailySet dailySet, Question question, byte seq, AnswerPos answerPos) {
        this.dailySet = dailySet;
        this.question = question;
        this.seq = seq;
        this.answerPos = answerPos;
    }
}
