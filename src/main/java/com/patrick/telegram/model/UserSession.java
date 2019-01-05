package com.patrick.telegram.model;

import com.patrick.telegram.service.RouteService;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "UserSession")
public class UserSession {
    public static final String KEYBOARD_BUTTON_DELIMITER = " ; ";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "user_id")
    private int userId;
    @Column(name = "lesson_id")
    private int lessonId;
    @OneToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @Column
    private String userKeyBoard;
    @Column
    private String userQuestion;
    @Column
    private boolean finished;
    @Column
    private boolean correct;
    @Column
    private Date startDate;

    public UserSession() {
    }

    public UserSession(User user, Question question, Lesson lesson) {
        userId = user.getId();
        lessonId = lesson.getId();
        this.question = question;

        userKeyBoard = question.getKeyboard();
        userQuestion = "";
        startDate = new Date();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCorrect() {
        correct = question.getQuestion().equals(userQuestion);
        return correct;
    }

    public Question getQuestion() {
        return question;
    }

    public String getCorrectQuestion() {
        return question.getQuestion();
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public String getUserKeyBoard() {
        return userKeyBoard;
    }

    public List<String> getUserKeyBoardButtons() {
        if (userKeyBoard.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(toKeyBoard(userKeyBoard));
    }

    private List<String> toKeyBoard(String userKeyBoard) {
        return Arrays.asList(userKeyBoard.split(KEYBOARD_BUTTON_DELIMITER));
    }

    private String fromKeyBoard(List<String> keyBoard) {
        return String.join(KEYBOARD_BUTTON_DELIMITER, keyBoard);
    }

    public void process(String chosenButton) {
        if (RouteService.CHECK_QUESTION.equals(chosenButton)) {
            finished = true;
            return;
        }
        String delimiter = userQuestion.isEmpty() ? "" : " ";

        if (!userKeyBoard.contains(chosenButton)) {
            return;
        }

        /* Collecting user input from keyBoard */
        userQuestion += delimiter + chosenButton;

        /* Remove buttons in user keyBoard */
        List<String> currentKeyBoard = toKeyBoard(userKeyBoard);
        List<String> updatedKeyBoard = new ArrayList<>();
        int count = 1;
        for (String button : currentKeyBoard) {
            if (count == 1 && button.equals(chosenButton)) {
                count--;
                continue;
            }
            updatedKeyBoard.add(button);
        }

        userKeyBoard = fromKeyBoard(updatedKeyBoard);
    }

    public void finishSession() {
        finished = true;
    }

    public int getId() {
        return id;
    }

    public int getLessonId() {
        return lessonId;
    }

    public int getUserId() {
        return userId;
    }

    public Date getStartDate() {
        return startDate;
    }
}
