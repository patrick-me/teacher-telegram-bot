package com.patrick.telegram.model;

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Patrick on 24.02.2018.
 */
@Entity
@Table(name = "Answer")
public class Answer implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    @BatchSize(size = 25)
    @OneToOne
    @JoinColumn(name = "sentence_id")
    private Sentence sentence;
    @OneToOne
    @JoinColumn(name = "questionType_id")
    private QuestionType questionType;

    @Column(name = "highlightedSentence")
    private String highlightedSentence;
    @Column(name = "question")
    private String question;
    @Column(name = "keyboard")
    private String keyboard;
}