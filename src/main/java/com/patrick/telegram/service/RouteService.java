package com.patrick.telegram.service;

import com.patrick.telegram.model.Lesson;
import com.patrick.telegram.model.Question;
import com.patrick.telegram.model.QuestionType;
import com.patrick.telegram.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
public class RouteService {
    private static final String NEXT_QUESTION = "Следующий вопрос";
    private static final String FINISH_LESSON = "Завершить урок";
    public static final String CHECK_QUESTION = "Проверить вопрос";

    private static final String START_CMD = "Начать";
    private static final String LESSONS_CMD = "Уроки";
    private static final String SECRET_CMD = "/secret";


    private final BotService botService;
    private final UserService userService;
    private final LessonService lessonService;
    private final QuestionService questionService;
    private final UserSessionService userSessionService;

    @Autowired
    public RouteService(BotService botService, UserService userService, LessonService lessonService,
                        QuestionService questionService, UserSessionService userSessionService) {
        this.botService = botService;
        this.userService = userService;
        this.lessonService = lessonService;
        this.questionService = questionService;
        this.userSessionService = userSessionService;
    }


    public void route(int botId, Update update) {

        boolean hasMessage = update.hasMessage();
        com.patrick.telegram.model.User user;

        if (hasMessage) {
            User tgUser = update.getMessage().getFrom();
            user = userService.getUserByTID(tgUser.getId());

            if (user == null) {
                com.patrick.telegram.model.User createdUser = new com.patrick.telegram.model.User(tgUser);
                userService.saveUser(createdUser);
                user = createdUser;
            }
            userService.updateLastLogin(user.getId());

            if (update.getMessage().hasEntities()) {
                processCommands(botId, update.getMessage().getChatId(), update.getMessage().getEntities(), user);
            } else if (update.getMessage().hasText()) {
                processCommand(botId, update.getMessage().getChatId(), update.getMessage().getText(), user);
            }
        }
    }

    private void processCommands(int botId, Long chatId, List<MessageEntity> entities, com.patrick.telegram.model.User user) {
        for (MessageEntity e : entities) {
            processCommand(botId, chatId, e.getText(), user);
        }
    }

    private void processCommand(int botId, Long chatId, String newMessage, com.patrick.telegram.model.User user) {
        System.out.println("last: " + user.getLastMessage());
        System.out.println("new: " + newMessage);

        UserSession userSession = userSessionService.getSession(user.getId());

        if (userSession != null) {
            switch (newMessage) {
                case FINISH_LESSON:
                    userSession.finishSession();
                    sendMessage(botId, chatId,
                            "Here, you could see lessons which Panda-trainer assigned to you",
                            getLessonKeyBoard(user.getId())
                    );
                    break;
                case NEXT_QUESTION:
                    userSession.finishSession();
                    processChosenLesson(user, chatId, botId, lessonService.getLesson(userSession.getLessonId()));
                    break;
                case CHECK_QUESTION:
                    String checkMessage = (userSession.isCorrect()) ? "*Correct!*" : "*Fail!*" +
                            "\nCorrect question: " + userSession.getCorrectQuestion() +
                            "\nYour question: " + userSession.getUserQuestion();
                    sendMessage(botId, chatId, checkMessage, getFinishKeyBoard());
                    sendPanda(botId, chatId, userSession.isCorrect() ? "panda1.jpg" : "panda2.jpg");
                    //TODO define commands and pictures
                    break;
                default:
                    userSession.process(newMessage);
                    sendMessage(
                            botId, chatId, userSession.getUserQuestion(), userSession.getUserKeyBoardButtons(),
                            getCheckKeyBoard()
                    );
            }
            userSessionService.save(userSession);

        } else if (Arrays.asList(FINISH_LESSON, LESSONS_CMD).contains(user.getLastMessage())) {
            processChosenLesson(user, chatId, botId, lessonService.getUserLessonByName(user.getId(), newMessage));
        } else {

            switch (newMessage) {
                case START_CMD:
                    sendMessage(botId, chatId, "Welcome to Panda's Question bot", getStartKeyBoard());
                    sendPanda(botId, chatId, "panda0.jpg");
                    break;
                case LESSONS_CMD:
                    sendMessage(botId, chatId,
                            "Here, you could see lessons which Panda-trainer assigned to you",
                            getLessonKeyBoard(user.getId())
                    );
                    break;
                case SECRET_CMD:
                    sendMessage(botId, chatId,
                            "It's a secret: " +
                                    new String(Character.toChars(128515)) + "\u27a1" + "\u27a2" + "\u27a3" + "\u27a4" + "\u27a5" + "\u27a6" + "\u27a7" + " His younger *BROTHER* is in love with her older sister. → "
                    );
                    break;
                default:
                    sendMessage(botId, chatId, "Welcome to Panda's Question bot", getStartKeyBoard());
            }
        }

        user.addMessage(newMessage);
        userService.saveUser(user);
    }

    private void processChosenLesson(com.patrick.telegram.model.User user, Long chatId, int botId, Optional<Lesson> oLesson) {
        if (!oLesson.isPresent()) {
            return;
        }

        Lesson lesson = oLesson.get();
        UserSession userSession = new UserSession(user, getRandomQuestion(lesson), lesson);
        userSessionService.save(userSession);

        sendMessage(botId, chatId, lesson.getDescription());
        sendMessage(botId, chatId, userSession.getQuestion().getHighlightedSentence(),
                userSession.getUserKeyBoardButtons(), getCheckKeyBoard());
    }

    private void sendMessage(int botId, long chatId, String text) {
        sendMessage(botId, chatId, text, Collections.emptyList());
    }

    private void sendMessage(int botId, long chatId, String text, List<String> keyBoardButtons) {
        sendMessage(botId, chatId, text, keyBoardButtons, Collections.emptyList());
    }

    private void sendMessage(int botId, long chatId, String text, List<String> keyBoardButtons,
                             List<String> keyBoardControlButtons) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboard = getKeyBoard(keyBoardButtons, keyBoardControlButtons);

        if (!keyBoardButtons.isEmpty() || !keyBoardControlButtons.isEmpty()) {
            message.setReplyMarkup(replyKeyboard);
        }
        botService.send(botId, message);
    }

    private void sendPanda(int botId, long chatId, String pandaFile) {

        try {

            InputStream initialStream = new FileInputStream(
                    new File("src/main/resources/" + pandaFile));
            SendPhoto message = new SendPhoto()
                    .setChatId(chatId)
                    .setPhoto(pandaFile, initialStream);
            botService.send(botId, message);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private Question getRandomQuestion(Lesson lesson) {
        int index = ThreadLocalRandom.current().nextInt(lesson.getQuestionTypes().size());
        QuestionType questionType = new ArrayList<>(lesson.getQuestionTypes()).get(index);

        List<Question> questions = questionService.getQuestions().stream()
                .filter(qs -> qs.getQuestionType().getId() == questionType.getId())
                .collect(Collectors.toList());

        if (questions.isEmpty()) {
            return getRandomQuestion(lesson);
        }

        index = ThreadLocalRandom.current().nextInt(questions.size());
        return questions.get(index);
    }

    private ReplyKeyboardMarkup getKeyBoard(List<String> keyboardElements, List<String> keyboardControlElements) {
        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row
        addRowsToKeyBoard(keyboard, keyboardElements, 3);
        addRowsToKeyBoard(keyboard, keyboardControlElements, 3);

        // Set the keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void addRowsToKeyBoard(List<KeyboardRow> keyboard, List<String> elements,
                                   int numberOfElementsPerRow) {
        int count = numberOfElementsPerRow;
        KeyboardRow row = new KeyboardRow();
        for (String element : elements) {
            if (count-- < 1) {
                keyboard.add(row);
                row = new KeyboardRow();
                count = numberOfElementsPerRow - 1;
            }
            row.add(element);
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }
    }

    private List<String> getLessonKeyBoard(int userId) {
        return lessonService.getUserLessons(userId).stream()
                .map(Lesson::getName)
                .collect(Collectors.toList());
    }

    private List<String> getCheckKeyBoard() {
        return Arrays.asList(CHECK_QUESTION, FINISH_LESSON);
    }

    private List<String> getFinishKeyBoard() {
        return Arrays.asList(NEXT_QUESTION, FINISH_LESSON);
    }

    private List<String> getStartKeyBoard() {
        return Arrays.asList(START_CMD, LESSONS_CMD);
    }
}