package com.patrick.telegram.service;

import com.patrick.telegram.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
public class RouteService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String NEXT_QUESTION = "Следующий вопрос";
    private static final String FINISH_LESSON = "Завершить урок";
    public static final String CHECK_QUESTION = "Проверить вопрос";

    private static final String LESSONS_CMD = "Уроки";


    private final BotService botService;
    private final UserService userService;
    private final LessonService lessonService;
    private final QuestionService questionService;
    private final UserSessionService userSessionService;
    private final PandaService pandaService;

    @Autowired
    public RouteService(BotService botService, UserService userService, LessonService lessonService,
                        QuestionService questionService, UserSessionService userSessionService, PandaService pandaService) {
        this.botService = botService;
        this.userService = userService;
        this.lessonService = lessonService;
        this.questionService = questionService;
        this.userSessionService = userSessionService;
        this.pandaService = pandaService;
    }

    public void route(int botId, Update update) {
        try {
            process(botId, update);
        } catch (Exception e) {
            log.error("processing failed", e);
            e.printStackTrace();
        }
    }

    private void process(int botId, Update update) {
        boolean hasMessage = update.hasMessage();
        User user;

        log.info("Processing");
        if (hasMessage) {
            user = userService.getUser(update.getMessage().getFrom());

            log.info("Processing, User: {}", user.getLastName());
            if (update.getMessage().hasEntities()) {
                processCommands(botId, update.getMessage().getChatId(), update.getMessage().getEntities(), user);
            } else if (update.getMessage().hasText()) {
                processCommand(botId, update.getMessage().getChatId(), update.getMessage().getText(), user);
            }
        }
    }

    private void processCommands(int botId, Long chatId, List<MessageEntity> entities, User user) {
        for (MessageEntity e : entities) {
            processCommand(botId, chatId, e.getText(), user);
        }
    }

    private void processCommand(int botId, Long chatId, String newMessage, User user) {
        UserSession userSession = userSessionService.getActiveSession(user.getId());

        //TODO: new method

        log.info("Processing, LastMessage: {}, NewMessage: {}", user.getLastMessage(), newMessage);
        /* User has studied an active lesson */
        if (userSession != null) {
            switch (newMessage) {
                case FINISH_LESSON:
                    userSession.finishSession();
                    sendMessage(botId, chatId,
                            "Here, you could see lessons which Panda-trainer assigned to you",
                            getStartKeyBoard()
                    );
                    break;
                case NEXT_QUESTION:
                    userSession.finishSession();
                    processChosenLesson(user, chatId, botId, lessonService.getLesson(userSession.getLessonId()), false);
                    break;
                case CHECK_QUESTION:
                    String checkMessage = (userSession.isCorrect()) ? "*Correct!*" : "*Fail!*" +
                            "\nCorrect question: " + userSession.getCorrectQuestion() +
                            "\nYour question: " + userSession.getUserQuestion();
                    sendMessage(botId, chatId, checkMessage, getFinishKeyBoard());
                    sendPanda(botId, chatId, user.getId(), userSession);
                    //TODO define commands
                    break;
                default:
                    userSession.process(newMessage);
                    sendMessage(
                            botId, chatId, userSession.getUserQuestion(), userSession.getUserKeyBoardButtons(),
                            getCheckKeyBoard()
                    );
            }
            userSessionService.save(userSession);

        } else if (LESSONS_CMD.equals(user.getLastMessage()) && !LESSONS_CMD.equals(newMessage)) {
            /* User has started a lesson */
            processChosenLesson(user, chatId, botId, lessonService.getUserLessonByName(user.getId(), newMessage), true);
        } else {
            /* First user access or after finish lesson */
            switch (newMessage) {
                case LESSONS_CMD:
                    if (hasLessons(user.getId())) {
                        sendMessage(botId, chatId,
                                "Here, you could see lessons which Panda-trainer assigned to you",
                                getLessonKeyBoard(user.getId())
                        );
                        break;
                    }
                    sendMessage(botId, chatId, "Ask your teacher about new lessons for you", getStartKeyBoard());
                default:
                    sendMessage(botId, chatId, "Welcome to Panda's Question bot", getStartKeyBoard());
            }
        }

        user.addMessage(newMessage);
        userService.saveUser(user);
    }

    private void processChosenLesson(User user, Long chatId, int botId, Optional<Lesson> oLesson, boolean sendDesc) {
        if (!oLesson.isPresent()) {
            return;
        }

        Lesson lesson = oLesson.get();
        UserSession userSession = new UserSession(user, getRandomQuestion(user, lesson), lesson);
        userSessionService.save(userSession);

        if (sendDesc) {
            sendMessage(botId, chatId, lesson.getDescription());
        }
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
        if (StringUtils.isEmpty(text)) {
            return;
        }

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

    private void sendPanda(int botId, long chatId, int userId, UserSession userSession) {
        if (shouldSendPanda(userId, userSession)) {
            Optional<Panda> panda = userSession.isCorrect() ? pandaService.getPositivePanda() : pandaService.getNegativePanda();
            panda.ifPresent(p ->
                    sendPanda(
                            botId,
                            chatId,
                            pandaService.getPandaInputStream(p)
                    ));
        }
    }

    private boolean shouldSendPanda(int userId, UserSession userSession) {
        int frequency = pandaService.getNumberHowOftenSendPanda();
        return userSessionService.isLastNumberOfSessionsCorrect(
                userSession.isCorrect(),
                frequency,
                userSession.getLessonId(),
                userId
        );
    }

    private void sendPanda(int botId, long chatId, InputStream is) {
        try {
            SendPhoto message = new SendPhoto()
                    .setChatId(chatId)
                    .setPhoto("Panda", is);
            botService.send(botId, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Question getRandomQuestion(User user, Lesson lesson) {
        return questionService.getRandomQuestion(user.getId(), lesson.getId());
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

    private boolean hasLessons(int userId) {
        return !lessonService.getUserLessons(userId).isEmpty();
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
        return Collections.singletonList(LESSONS_CMD);
    }
}
