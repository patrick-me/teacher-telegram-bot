package com.patrick.telegram.model;

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
    @Column
    private Integer botReplyMessageId;

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

    public void finishSession() {
        isCorrect();
        finished = true;
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

    public boolean process(String chosenButton) {
        String delimiter = userQuestion.isEmpty() ? "" : " ";

        if (userKeyBoard == null || !userKeyBoard.contains(chosenButton)) {
            return false;
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
        return true;
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

    public boolean hasBotReplyMessageId() {
        return null != botReplyMessageId;
    }

    public Integer getBotReplyMessageId() {
        return botReplyMessageId;
    }

    public void setBotReplyMessageId(Integer botReplyMessageId) {
        this.botReplyMessageId = botReplyMessageId;
    }
}
