package com.patrick.telegram.service;

import com.google.common.collect.ImmutableList;
import com.patrick.telegram.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
public class RouteService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String NEXT_PROBLEM = "Следующее задание";
    private static final String FINISH_LESSON = "Завершить урок";
    private static final String CHECK = "Проверить";

    private static final String LESSONS_CMD = "Уроки";

    private static final String START_LESSON = "Начать урок";
    private static final String LESSON_STAT_CMD = "Статистика урока";
    private static final String LESSON_DESC_CMD = "Описание урока";
    private static final String BACK_TO_LESSONS_CMD = "К урокам";

    private static final String FAQ = "FAQ";

    private static final String SUPPORT_USERS_CMD = "/users";

    private static final List<String> REACTIONS_ON_CORRECT_ANSWER = ImmutableList.of(
            "Великолепно!", "Просто супер!", "Правильно!", "В яблочко!", "И это верно!",
            "Вы сегодня просто в ударе!", "Красота!", "Perfecto!", "+1", "Верно, продолжайте в том же духе!"
    );
    private static final List<String> REACTIONS_ON_FAILED_ANSWER = ImmutableList.of(
            "В другой раз точно повезет!", "Сделай работу над ошибками!", "Почти получилось...", "Смотри как нужно", "Не правильно",
            "Панда негодует", "Не расстраивайся", "Плохие новости", "Сегодня не везет", "Старайся лучше"
    );
    private Random randomGenerator = new Random();

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

            log.info("Processing, User: {} {} [{}]", user.getLastName(), user.getFirstName(), user.getNickName());
            if (update.getMessage().hasEntities()) {
                processCommands(botId, update.getMessage(), update.getMessage().getEntities(), user);
            } else if (update.getMessage().hasText()) {
                processCommand(botId, update.getMessage(), update.getMessage().getText(), user);
            }
        }
    }

    private void processCommands(int botId, org.telegram.telegrambots.meta.api.objects.Message message, List<MessageEntity> entities, User user) {
        for (MessageEntity e : entities) {
            processCommand(botId, message, e.getText(), user);
        }
    }

    private void processCommand(int botId, org.telegram.telegrambots.meta.api.objects.Message message, String newMessage, User user) {
        Long chatId = message.getChatId();
        Optional<UserSession> oUserSession = userSessionService.getActiveSession(user.getId());
        String userLastMessage = messageService.getLastMessage(user.getId());
        //findUserPhotos(botId, user.getTelegramId());

        log.info("Processing, LastMessage: {}, NewMessage: {}", userLastMessage, newMessage);

        if (oUserSession.isPresent()) {
            /* User has studied an active lesson */
            processActiveUserSession(botId, message, newMessage, user, chatId, oUserSession.get());
        } else if (
                (LESSONS_CMD.equals(userLastMessage) && !LESSONS_CMD.equals(newMessage)) ||
                        (BACK_TO_LESSONS_CMD.equals(userLastMessage) && !BACK_TO_LESSONS_CMD.equals(newMessage))
        ) {
            processOpenLesson(user, chatId, botId, newMessage);
        } else if (user.isAdmin() && SUPPORT_USERS_CMD.equals(userLastMessage) && !SUPPORT_USERS_CMD.equals(newMessage)) {
            processUserSupport(user, chatId, botId, newMessage);
        } else {
            /* First user access or after finish lesson */
            processStartOrReturnToStartPoint(botId, newMessage, user, chatId);
        }

        messageService.save(newMessage, user.getId());
        userService.saveUser(user);
    }

    private void processStartOrReturnToStartPoint(int botId, String newMessage, User user, Long chatId) {
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
                        getStartKeyBoard(user)
                );
                break;
            case SUPPORT_USERS_CMD:
                if (user.isAdmin()) {
                    sendMessage(botId, chatId, "Выберите пользователя", getUsersForKeyBoard());
                }
                break;
            case FAQ:
                sendMessage(botId, chatId, configService.getCommandDescription(FAQ, "Тут будет много текста, приходи в другой раз"));
                break;
            default:
                sendMessage(botId, chatId, configService.getCommandDescription("Приветствие", "Рад видеть, давай учиться!"), getStartKeyBoard(user));
        }
    }

    private void processActiveUserSession(int botId, org.telegram.telegrambots.meta.api.objects.Message message, String newMessage, User user, Long chatId, UserSession userSession) {
        switch (newMessage) {
            case START_LESSON:
            case NEXT_PROBLEM:
                userSession.finishSession();
                processChosenLesson(user, chatId, botId, userSession);
                break;
            case LESSON_DESC_CMD:
                sendMessage(botId, chatId, getLessonDesc(userSession));
                break;
            case LESSON_STAT_CMD:
                sendMessage(botId, chatId, getLessonStats(userSession));
                break;
            case FINISH_LESSON:
                userSession.finishSession();
                sendMessage(botId, chatId,
                        configService.getCommandDescription(FINISH_LESSON, "Возвращайся поскорее!"),
                        getStartKeyBoard(user)
                );
                break;
            case CHECK:
                String correctDescValue = configService.getCommandDescription(CHECK + " " + "(При верном ответе)", "");
                String incorrectDescValue = configService.getCommandDescription(CHECK + " " + "(При неверном ответе)", "");
                String incorrectDescValue1 = configService.getCommandDescription(CHECK + " " + "(При неверном ответе) - перед выводом правильного ответа", "");
                String incorrectDescValue2 = configService.getCommandDescription(CHECK + " " + "(При неверном ответе) - перед выводом введенного ответа", "");

                String correctDesc = "".equals(correctDescValue) ? getDefaultReactionOnCorrectAnswer() : correctDescValue;
                String incorrectDesc = "".equals(incorrectDescValue) ? getDefaultReactionOnFailedAnswer() : incorrectDescValue;
                String incorrectDesc1 = "".equals(incorrectDescValue1) ? "_Ожидается_:" : incorrectDescValue1;
                String incorrectDesc2 = "".equals(incorrectDescValue2) ? "_Ваш ответ_:" : incorrectDescValue2;

                String checkMessage = (userSession.isCorrect()) ? correctDesc :
                        incorrectDesc + "\n\n" +
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
                boolean successfulProcessed = userSession.process(newMessage);

                if (successfulProcessed) {
                    if (userSession.hasBotReplyMessageId()) {
                        editMessage(botId, chatId, userSession.getBotReplyMessageId(), userSession.getUserQuestion());
                    } else {
                        Optional<org.telegram.telegrambots.meta.api.objects.Message> oSentMessage = sendMessage(
                                botId, chatId, userSession.getUserQuestion()
                        );
                        if (oSentMessage.isPresent()) {
                            org.telegram.telegrambots.meta.api.objects.Message sentMessage = oSentMessage.get();
                            userSession.setBotReplyMessageId(sentMessage.getMessageId());
                        }
                    }
                }

                deleteMessage(botId, chatId, message.getMessageId());

        }
        userSessionService.save(userSession);
    }

    private String getDefaultReactionOnFailedAnswer() {
        int next = randomGenerator.nextInt(REACTIONS_ON_FAILED_ANSWER.size());
        return REACTIONS_ON_FAILED_ANSWER.get(next);
    }

    private String getDefaultReactionOnCorrectAnswer() {
        int next = randomGenerator.nextInt(REACTIONS_ON_CORRECT_ANSWER.size());
        return REACTIONS_ON_CORRECT_ANSWER.get(next);
    }

    private String getLessonStats(UserSession userSession) {

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
        StringBuilder statistic = new StringBuilder("Статистика урока: *'" + lesson.getName() + "'*" + ln + ln);
        statistic.append("Кол-во предложений: *").append(sentences.size()).append("*").append(ln);
        statistic.append("Кол-во заданий: *").append(questions.size()).append("*").append(ln);
        statistic.append("Кол-во правильно решенных заданий: *")
                .append(userSuccessfulAnsweredQuestions.size()).append("/").append(questions.size())
                .append("*").append(ln);
        return statistic.toString();
    }

    //user firstname lastname\nuser_id
    //chosen user expected - to show stats and lastLogin
    private void processUserSupport(User user, Long chatId, int botId, String newMessage) {
        String chosenUserId = newMessage.split("\n")[1];
        log.info("UserId: '{}'", chosenUserId);

        User chosenUser = userService.getUserById(Integer.parseInt(chosenUserId));
        if (chosenUser == null) {
            log.error("User is null, chosenUserId: '{}'", chosenUserId);
            return;
        }

        StringBuilder userInfo = new StringBuilder();
        String userLessonsWithProgress = getLessonsKeyBoard(chosenUser.getId()).stream()
                .sorted()
                .map(l -> l.replace("\n", " - "))
                .collect(Collectors.joining("\n"));

        userInfo.append("User: ").append(String.format("%s %s", chosenUser.getFirstName(), chosenUser.getLastName())).append("\n")
                .append("NickName: ").append(chosenUser.getNickName()).append("\n")
                .append("Last seen: ").append(chosenUser.getLastLogin()).append("\n")
                .append("Assigned lessons:\n").append(userLessonsWithProgress);

        sendMessage(botId, chatId, userInfo.toString(), false, getStartKeyBoard(user), Collections.emptyList());

    }

    private void processOpenLesson(User user, Long chatId, int botId, String newMessage) {
        String lessonName = newMessage.contains("\n") ? newMessage.split("\n")[0] : newMessage;
        log.info("Opening lesson: '{}'", lessonName);

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

    private void processChosenLesson(User user, Long chatId, int botId, UserSession currentUserSession) {
        Optional<Lesson> oLesson = lessonService.getLesson(currentUserSession.getLessonId());
        if (!oLesson.isPresent()) {
            return;
        }

        Lesson lesson = oLesson.get();
        UserSession userSession = new UserSession(user, getRandomQuestion(user, lesson), lesson);
        userSessionService.save(userSession);

        sendMessage(botId, chatId, userSession.getQuestion().getHighlightedSentence(), true,
                userSession.getUserKeyBoardButtons(), getCheckKeyBoard());
    }

    private String getLessonDesc(UserSession currentUserSession) {
        Optional<Lesson> oLesson = lessonService.getLesson(currentUserSession.getLessonId());
        if (!oLesson.isPresent()) {
            return "There is no description yet";
        }

        Lesson lesson = oLesson.get();
        return lesson.getDescription();
    }

    private Optional<org.telegram.telegrambots.meta.api.objects.Message> sendMessage(int botId, long chatId, String text) {
        return sendMessage(botId, chatId, text, Collections.emptyList());
    }

    private Optional<org.telegram.telegrambots.meta.api.objects.Message> sendMessage(int botId, long chatId, String text, List<String> keyBoardButtons) {
        return sendMessage(botId, chatId, text, true, keyBoardButtons, Collections.emptyList());
    }

    private Optional<org.telegram.telegrambots.meta.api.objects.Message> sendMessage(int botId, long chatId, String text,
                                                                                     boolean enableMarkdown,
                                                                                     List<String> keyBoardButtons,
                                                                                     List<String> keyBoardControlButtons) {

        //TODO: check * and _ markdown

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(enableMarkdown);

        if (StringUtils.isEmpty(text)) {
            message.setText("There is no text yet");
        }

        ReplyKeyboardMarkup replyKeyboard = getKeyBoard(keyBoardButtons, keyBoardControlButtons);

        if (!keyBoardButtons.isEmpty() || !keyBoardControlButtons.isEmpty()) {
            message.setReplyMarkup(replyKeyboard);
        }
        return botService.send(botId, message);
    }

    private void editMessage(int botId, long chatId, int messageId, String text) {
        EditMessageText message = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .enableMarkdown(true);

        botService.send(botId, message);
    }

    private void deleteMessage(int botId, long chatId, int messageId) {
        DeleteMessage message = new DeleteMessage()
                .setChatId(chatId)
                .setMessageId(messageId);

        botService.send(botId, message);
    }

    private void findUserPhotos(int botId, int telegramUserId) {
        GetUserProfilePhotos userProfilePhotos = new GetUserProfilePhotos();
        userProfilePhotos = userProfilePhotos.setUserId(telegramUserId).setOffset(0).setLimit(10);

        String token = botService.getBotToken(botId);
        Optional<UserProfilePhotos> send = botService.send(botId, userProfilePhotos);
        System.out.println(send);
        send.ifPresent(p -> {
            GetFile file = new GetFile().setFileId(p.getPhotos().get(0).get(0).getFileId());
            Optional<File> f = botService.send(botId, file);
            f.ifPresent(q -> {
                        System.out.println(q.getFileUrl(token));
                        URL url = null;
                        try {
                            url = new URL(q.getFileUrl(token));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        try {
                            BufferedInputStream bis = new BufferedInputStream(url.openConnection().getInputStream());
                            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(bis);
                            Base64.getEncoder().encode(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );

        });
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

    private List<String> getUsersForKeyBoard() {
        return userService.getUsers().stream()
                .map(u -> String.format("%s %s\n%s", u.getFirstName(), u.getLastName(), u.getId()))
                .collect(Collectors.toList());
    }

    private List<String> getLessonsKeyBoard(int userId) {
        return lessonService.getUserLessons(userId).stream()
                .map(l -> {
                    String progress = getProgressByLesson(userId, l);
                    return l.getName() + progress;
                })
                .collect(Collectors.toList());
    }

    private String getProgressByLesson(int userId, Lesson l) {
        String progress = "";
        Collection<Question> questions = questionService.getQuestions(l.getId());
        Collection<Question> successfulAnsweredQuestions = questionService.getSuccessfulAnsweredQuestions(
                userId, l.getId());
        if (questions != null && !questions.isEmpty()
                && successfulAnsweredQuestions != null && !successfulAnsweredQuestions.isEmpty()) {
            int percent = 100 * successfulAnsweredQuestions.size() / questions.size();
            if (percent > 0) {
                progress = "\n" + percent + "%";
            }
        }
        return progress;
    }

    private List<String> getCheckKeyBoard() {
        return Arrays.asList(CHECK, FINISH_LESSON);
    }

    private List<String> getFinishKeyBoard() {
        return Arrays.asList(NEXT_PROBLEM, LESSON_STAT_CMD, LESSON_DESC_CMD, FINISH_LESSON);
    }

    private List<String> getStartKeyBoard(User user) {
        if (user.isAdmin()) {
            return Arrays.asList(LESSONS_CMD, FAQ, SUPPORT_USERS_CMD);
        } else {
            return Arrays.asList(LESSONS_CMD, FAQ);
        }
    }

    private List<String> getLessonKeyBoard() {
        return Arrays.asList(START_LESSON, LESSON_STAT_CMD, LESSON_DESC_CMD, BACK_TO_LESSONS_CMD);
    }
}
