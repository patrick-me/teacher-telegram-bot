package com.patrick.telegram.service;

import com.google.common.base.Strings;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
public class RouteService {

    public static final int REQUIRED_USER_LAST_LOGIN_TIME_IN_DAYS = 60;
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

    private static final String SUPPORT_ALL_USERS_CMD = "/all_users";
    private static final String SUPPORT_USERS_CMD = "/users";
    private static final String SUPPORT_LESSONS_CMD = "/lessons";


    private static final List<String> REACTIONS_ON_CORRECT_ANSWER = ImmutableList.of(
            "Великолепно!", "Просто супер!", "Правильно!", "В яблочко!", "И это верно!",
            "Вы сегодня просто в ударе!", "Bingo!", "Perfecto!", "+1", "Верно, продолжайте в том же духе!"
    );
    private static final List<String> REACTIONS_ON_FAILED_ANSWER = ImmutableList.of(
            "В другой раз точно повезет!", "Сделай работу над ошибками!", "Почти получилось...", "Смотри как нужно", "Не правильно",
            "Панда негодует", "Не расстраивайся", "Плохие новости", "Сегодня не везет", "Старайся лучше"
    );
    private Random randomGenerator = new Random();
    private static final SimpleDateFormat moscowDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        moscowDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

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
        } else if (user.isAdmin() && SUPPORT_ALL_USERS_CMD.equals(userLastMessage) && !SUPPORT_ALL_USERS_CMD.equals(newMessage)) {
            processUserSupport(user, chatId, botId, newMessage);
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
                        true,
                        getStartKeyBoard(),
                        getSupportKeyBoard(user)
                );
                break;
            case FAQ:
                sendMessage(botId, chatId, configService.getCommandDescription(FAQ, "Тут будет много текста, приходи в другой раз"));
                break;
            case SUPPORT_ALL_USERS_CMD:
            case SUPPORT_USERS_CMD:
                if (user.isAdmin()) {
                    //Show if user active last n days; 0 - show all users
                    int days = newMessage.equals(SUPPORT_ALL_USERS_CMD) ? 0 : REQUIRED_USER_LAST_LOGIN_TIME_IN_DAYS;
                    String extra = "\n" + ((days == 0)
                            ? "Показаны все пользователи"
                            : "Показаны все пользователи\nc активностью за последние " + REQUIRED_USER_LAST_LOGIN_TIME_IN_DAYS + " дней");
                    sendMessage(botId, chatId, "Выберите пользователя" + extra, getUsersForKeyBoard(days));
                    break;
                }
            case SUPPORT_LESSONS_CMD:
                if (user.isAdmin()) {
                    sendMessage(botId, chatId, "*Информация по урокам*\n" + getLessonsInfo(), true, getStartKeyBoard(), getSupportKeyBoard(user));
                    break;
                }
            default:
                sendMessage(botId, chatId, configService.getCommandDescription("Приветствие", "Рад видеть, давай учиться!"),
                        true, getStartKeyBoard(), getSupportKeyBoard(user));
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
                        true,
                        getStartKeyBoard(),
                        getSupportKeyBoard(user)
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
        if (!newMessage.contains("\n")) {
            sendMessage(botId, chatId, "Выберите пользователя", getUsersForKeyBoard(0));
            return;
        }
        String chosenUserId = newMessage.split("\n")[1];
        log.info("UserId: '{}'", chosenUserId);

        Optional<User> optionalUser = userService.getUserById(Integer.parseInt(chosenUserId));
        if (!optionalUser.isPresent()) {
            log.error("User is null, chosenUserId: '{}'", chosenUserId);
            return;
        }
        User chosenUser = optionalUser.get();

        StringBuilder userInfo = new StringBuilder();
        String userLessonsWithProgress = getLessonsKeyBoard(chosenUser.getId()).stream()
                .sorted()
                .map(l -> l.replace("\n", " - "))
                .collect(Collectors.joining("\n"));

        userInfo.append("User: ")
                .append(
                        String.format(
                                "%s %s",
                                Strings.nullToEmpty(chosenUser.getFirstName()),
                                Strings.nullToEmpty(chosenUser.getLastName()))).append("\n")
                .append("NickName: ").append(Strings.nullToEmpty(chosenUser.getNickName())).append("\n")
                .append("Last seen: ").append(moscowDateFormat.format(chosenUser.getLastLogin())).append("\n")
                .append("Assigned lessons:\n").append(userLessonsWithProgress);

        sendMessage(botId, chatId, userInfo.toString(), false, getStartKeyBoard(), getSupportKeyBoard(user));
        sendMessage(botId, chatId, getUserStatsForLastWeek(chosenUser), true, getStartKeyBoard(), getSupportKeyBoard(user));

    }

    private String getLessonsInfo() {
        StringBuilder lessonInfo = new StringBuilder();
        lessonService.getLessons()
                .forEach(l -> {
                    lessonInfo.append("Lesson: ").append(l.getName()).append("\n")
                            .append("How long is description: ").append(Strings.nullToEmpty(l.getDescription()).length()).append(" symbol(s)").append("\n")
                            .append("The number of the tasks: ").append(questionService.getQuestions(l.getId()).size()).append("\n\n");
                });
        return lessonInfo.toString();
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
        Optional<Question> optionalRandomQuestion = getRandomQuestion(user, lesson);

        if (!optionalRandomQuestion.isPresent()) {
            return;
        }

        UserSession userSession = new UserSession(user, optionalRandomQuestion.get(), lesson);
        userSessionService.save(userSession);

        sendMessage(botId, chatId, userSession.getQuestion().getHighlightedSentence(), true,
                userSession.getUserKeyBoardButtons(), getCheckKeyBoard());
    }

    private String getLessonDesc(UserSession currentUserSession) {
        Optional<Lesson> oLesson = lessonService.getLesson(currentUserSession.getLessonId());
        if (!oLesson.isPresent()) {
            return "There is no description here yet";
        }

        Lesson lesson = oLesson.get();
        return lesson.getDescription();
    }

    private String getUserStatsForLastWeek(User user) {
        int periodInDays = 7;
        Collection<UserStat> userStats = userSessionService.getUserStats(user.getId(), periodInDays);
        int totalSum = userStats.stream().mapToInt(UserStat::getTotalTaskCount).sum();

        if (totalSum == 0) {
            return "There are no statistics here";
        }

        int succeedSum = userStats.stream().mapToInt(UserStat::getSucceedTaskCount).sum();
        int failedSum = userStats.stream().mapToInt(UserStat::getFailedTaskCount).sum();

        int resizeLen = 6;
        StringBuilder sb = new StringBuilder("```\n\nСтатистика за " + periodInDays + " дней\n" +
                "Кол-во пройденных заданий\n'+' - succeed\n'-' - failed\n'T' - total\n\n")
                .append("\n" + "Date:      ")
                .append(resize("+", resizeLen))
                .append(resize("+%", resizeLen))
                .append(resize("-", resizeLen))
                .append(resize("-%", resizeLen))
                .append(resize("T", resizeLen))
                .append("\n");

        userStats.stream()
                .sorted(Comparator.comparing(UserStat::getStatDate))
                .forEach(us -> {
                    int succeedTaskCount = us.getSucceedTaskCount();
                    int failedTaskCount = us.getFailedTaskCount();
                    int totalTaskCount = us.getTotalTaskCount();

                    sb.append(us.getStatDate()).append(":")
                            .append(resize(succeedTaskCount, resizeLen))
                            .append(resize(100 * succeedTaskCount / totalTaskCount, resizeLen - 1)).append("%")
                            .append(resize(failedTaskCount, resizeLen))
                            .append(resize(100 - (100 * succeedTaskCount / totalTaskCount), resizeLen - 1)).append("%")
                            .append(resize(totalTaskCount, resizeLen)).append("\n");
                });

        sb.append("\n")
                .append("Total:     ")
                .append(resize(succeedSum, resizeLen))
                .append(resize(100 * succeedSum / totalSum, resizeLen - 1)).append("%")
                .append(resize(failedSum, resizeLen))
                .append(resize(100 - (100 * succeedSum / totalSum), resizeLen - 1)).append("%")
                .append(resize(totalSum, resizeLen)).append("\n```");

        return sb.toString();
    }


    private String resize(int i, int len) {
        return resize(String.valueOf(i), len);
    }

    private String resize(String i, int len) {
        StringBuilder s = new StringBuilder(String.valueOf(i));
        while (s.length() < len) {
            s.insert(0, " ");
        }
        return s.toString();
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
            message.setText("There is no text here yet");
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

    private Optional<Question> getRandomQuestion(User user, Lesson lesson) {
        return questionService.getRandomQuestion(user.getId(), lesson.getId());
    }

    private ReplyKeyboardMarkup getKeyBoard(List<String> keyboardElements, List<String> keyboardControlElements) {
        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row
        //TODO: number as param
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

    private List<String> getUsersForKeyBoard(int days) {
        Stream<User> stream = userService.getUsers().stream();
        if (days > 0) {
            stream = stream.filter(u -> Objects.nonNull(u.getLastLogin()))
                    .filter(u -> Duration.between(u.getLastLogin().toInstant(), Instant.now()).toDays() < days);

        }
        return stream
                .map(u -> String.format("%s %s\n%s", Strings.nullToEmpty(u.getFirstName()), Strings.nullToEmpty(u.getLastName()), u.getId()))
                .collect(Collectors.toList());
    }

    private List<String> getLessonsKeyBoard(int userId) {
        return lessonService.getUserLessons(userId).stream()
                .filter(l -> questionService.getQuestions(l.getId()).size() > 0)
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
        if (!questions.isEmpty() && !successfulAnsweredQuestions.isEmpty()) {
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

    private List<String> getStartKeyBoard() {
        return Arrays.asList(LESSONS_CMD, FAQ);
    }

    private List<String> getSupportKeyBoard(User user) {
        if (user.isAdmin()) {
            return Arrays.asList(SUPPORT_USERS_CMD, SUPPORT_ALL_USERS_CMD, SUPPORT_LESSONS_CMD);
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getLessonKeyBoard() {
        return Arrays.asList(START_LESSON, LESSON_STAT_CMD, LESSON_DESC_CMD, BACK_TO_LESSONS_CMD);
    }
}
