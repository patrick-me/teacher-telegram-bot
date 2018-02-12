package com.patrick.telegram.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Patrick on 26.01.2018.
 */
@Entity
@Table(name = "QUESTIONLESSON")
public class QuestionLesson extends Lesson {
    @Column(name = "question")
    private String question;
    @Column(name = "keyboard")
    private String keyboard;
    @Column(name = "answer")
    private String answer;

    public QuestionLesson() {
    }

    public QuestionLesson(int id, String group, String name, String question, String keyboard, String answer) {
        super(id, group, name);
        this.question = question;
        this.keyboard = keyboard;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getKeyboard() {
        return keyboard;
    }

    public String getAnswer() {
        return answer;
    }
}
