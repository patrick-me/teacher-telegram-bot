package com.patrick.telegram.model;

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Patrick on 24.02.2018.
 */
@Entity
@Table(name = "Question")
public class Question implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    @BatchSize(size = 25)
    @OneToOne
    @JoinColumn(name = "questionType_id")
    private QuestionType questionType;

    @Column(name = "highlightedSentence")
    private String highlightedSentence;
    @Column(name = "question")
    private String question;
    @Column(name = "keyboard")
    private String keyboard;

    public Question() {
    }

    public Question(QuestionType questionType, String highlightedSentence, String question, String keyboard) {
        this.questionType = questionType;
        this.highlightedSentence = highlightedSentence;
        this.question = question;
        this.keyboard = keyboard;
    }

    public int getId() {
        return id;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getHighlightedSentence() {
        return highlightedSentence;
    }

    public String getQuestion() {
        return question;
    }

    public String getKeyboard() {
        return keyboard;
    }
}