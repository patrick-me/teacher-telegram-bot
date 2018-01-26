package com.patrick.telegram.model;

/**
 * Created by Patrick on 26.01.2018.
 */
public class QuestionLesson extends Lesson {
    private String question;
    private String keyboard;
    private String answer;

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
