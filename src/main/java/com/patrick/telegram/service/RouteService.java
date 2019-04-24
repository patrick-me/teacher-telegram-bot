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

    private static final String START_LESSON = "Начать урок";
    private static final String LESSON_STAT_CMD = "Статистика урока";
    private static final String BACK_TO_LESSONS_CMD = "К урокам";

    private static final String FAQ = "FAQ";


    private final BotService botService;
    private final UserService userService;
    private final LessonService lessonService;
    private final SentenceService sentenceService;
    private final QuestionService questionService;
    private final UserSessionService userSessionService;
    private final PandaService pandaService;
    private final ConfigService configService;
    private final MessageService messageService;

    @Autowired
    public RouteService(SentenceService sentenceService, BotService botService, UserService userService, LessonService lessonService,
                        QuestionService questionService, UserSessionService userSessionService,
                        PandaService pandaService, ConfigService configService, MessageService messageService) {
        this.sentenceService = sentenceService;
        this.botService = botService;
        this.userService = userService;
        this.lessonService = lessonService;
        this.questionService = questionService;
        this.userSessionService = userSessionService;
        this.pandaService = pandaService;
        this.configService = configService;
        this.messageService = messageService;
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
        String userLastMessage = messageService.getLastMessage(user.getId());

        //TODO: new method
        log.info("Processing, LastMessage: {}, NewMessage: {}", userLastMessage, newMessage);
        /* User has studied an active lesson */
        if (userSession != null) {
            switch (newMessage) {
                case START_LESSON:
                    /* User has started a lesson */
                    userSession.finishSession();
                    processChosenLesson(user, chatId, botId, userSession, true);
                    break;
                case NEXT_QUESTION:
                    userSession.finishSession();
                    processChosenLesson(user, chatId, botId, userSession, false);
                    break;
                case LESSON_STAT_CMD:
                    sendMessage(botId, chatId, getStats(userSession));
                    break;
                case FINISH_LESSON:
                    userSession.finishSession();
                    sendMessage(botId, chatId,
                            configService.getCommandDescription(FINISH_LESSON, "Возвращайся поскорее!"),
                            getStartKeyBoard()
                    );
                    break;
                case CHECK_QUESTION:
                    String correctDesc = configService.getCommandDescription(CHECK_QUESTION + " " + "(При верном ответе)", "Великолепно!");
                    String incorrectDesc = configService.getCommandDescription(CHECK_QUESTION + " " + "(При неверном ответе)", "Нужно подучиться, в другой раз точно повезет!");
                    String incorrectDesc1 = configService.getCommandDescription(CHECK_QUESTION + " " + "(При неверном ответе) - перед выводом правильного ответа", "Вот как надо было:");
                    String incorrectDesc2 = configService.getCommandDescription(CHECK_QUESTION + " " + "(При неверном ответе) - перед выводом введенного ответа", "А вот так не надо:");

                    String checkMessage = (userSession.isCorrect()) ? correctDesc :
                            incorrectDesc + "\n" +
                                    incorrectDesc1 + " " + userSession.getCorrectQuestion() + "\n" +
                                    incorrectDesc2 + " " + userSession.getUserQuestion();

                    sendMessage(botId, chatId, checkMessage, getFinishKeyBoard());
                    sendPanda(botId, chatId, user.getId(), userSession);
                    break;
                case BACK_TO_LESSONS_CMD:
                    userSession.finishSession();
                    sendMessage(botId, chatId,
                            configService.getCommandDescription(LESSONS_CMD + " (Когда у пользователя есть назначенные уроки)", "Посмотри сколько у тебя уроков!\nДавай учиться!"),
                            getLessonsKeyBoard(user.getId())
                    );
                    break;
                default:
                    userSession.process(newMessage);
                    sendMessage(
                            botId, chatId, userSession.getUserQuestion(), userSession.getUserKeyBoardButtons(),
                            getCheckKeyBoard()
                    );
            }
            userSessionService.save(userSession);

        } else if (
                (LESSONS_CMD.equals(userLastMessage) && !LESSONS_CMD.equals(newMessage)) ||
                        (BACK_TO_LESSONS_CMD.equals(userLastMessage) && !BACK_TO_LESSONS_CMD.equals(newMessage))
        ) {
            processOpenLesson(user, chatId, botId, newMessage);
        } else {
            /* First user access or after finish lesson */
            switch (newMessage) {
                case LESSONS_CMD:
                    if (hasLessons(user.getId())) {
                        sendMessage(botId, chatId,
                                configService.getCommandDescription(LESSONS_CMD + " (Когда у пользователя есть назначенные уроки)", "Посмотри сколько у тебя уроков!\nДавай учиться!"),
                                getLessonsKeyBoard(user.getId())
                        );
                        break;
                    }
                    sendMessage(
                            botId,
                            chatId,
                            configService.getCommandDescription(
                                    LESSONS_CMD + " (Когда у пользователя нет назначеных уроков)",
                                    "Кажется, у тебя еще нет уроков. Спроси учителя об этом."
                            ),
                            getStartKeyBoard()
                    );
                    break;
                case FAQ:
                    sendMessage(botId, chatId, configService.getCommandDescription(FAQ, "Тут будет много текста, приходи в другой раз"));
                    break;
                default:
                    sendMessage(botId, chatId, configService.getCommandDescription("Приветствие", "Рад видеть, давай учиться!"), getStartKeyBoard());
            }
        }

        messageService.save(newMessage, user.getId());
        userService.saveUser(user);
    }

    private String getStats(UserSession userSession) {

        int lessonId = userSession.getLessonId();
        int userId = userSession.getUserId();
        Optional<Lesson> oLesson = lessonService.getLesson(lessonId);

        if (!oLesson.isPresent()) {
            return "No lesson: no statistic";
        }

        Lesson lesson = oLesson.get();
        Collection<Sentence> sentences = sentenceService.getSentences(lessonId);
        Collection<Question> questions = questionService.getQuestions(lessonId);
        Collection<Question> userSuccessfulAnsweredQuestions = questionService.getSuccessfulAnsweredQuestions(userId, lessonId);

        String ln = System.lineSeparator();
        StringBuilder statistic = new StringBuilder("Статистика урока: *'" + lesson.getName() + "'*" +ln);
        statistic.append("Кол-во предложений: *" + sentences.size() + "*" + ln);
        statistic.append("Кол-во вопросов: *" + questions.size() + "*" + ln);
        statistic.append(
                "Кол-во правильно отвеченных вопросов: *" +
                        userSuccessfulAnsweredQuestions.size() + "/" + questions.size() + "*" + ln
        );
        return statistic.toString();
    }

    private void processOpenLesson(User user, Long chatId, int botId, String lessonName) {
        Optional<Lesson> oLesson = lessonService.getUserLessonByName(user.getId(), lessonName);
        if (!oLesson.isPresent()) {
            return;
        }

        Lesson lesson = oLesson.get();
        //Stub session to remember opened lesson
        UserSession userSession = new UserSession(user, questionService.getQuestionStub(), lesson);
        userSessionService.save(userSession);
        sendMessage(botId, chatId, configService.getCommandDescription("Выбран урок", "Хороший выбор!"), getLessonKeyBoard());
    }

    private void processChosenLesson(User user, Long chatId, int botId, UserSession currentUserSession, boolean sendDesc) {
        Optional<Lesson> oLesson = lessonService.getLesson(currentUserSession.getLessonId());
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

        //TODO: check * and _ markdown

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(true);

        if (StringUtils.isEmpty(text)) {
            message.setText("EMPTY TEXT - CHANGE ME");
        }

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

    private List<String> getLessonsKeyBoard(int userId) {
        return lessonService.getUserLessons(userId).stream()
                .map(Lesson::getName)
                .collect(Collectors.toList());
    }

    private List<String> getCheckKeyBoard() {
        return Arrays.asList(CHECK_QUESTION, FINISH_LESSON);
    }

    private List<String> getFinishKeyBoard() {
        return Arrays.asList(NEXT_QUESTION, LESSON_STAT_CMD, FINISH_LESSON);
    }

    private List<String> getStartKeyBoard() {
        return Arrays.asList(LESSONS_CMD, FAQ);
    }

    private List<String> getLessonKeyBoard() {
        return Arrays.asList(START_LESSON, LESSON_STAT_CMD, BACK_TO_LESSONS_CMD);
    }
}
