package com.patrick.telegram.service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.patrick.telegram.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

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

    public static final int MAX_MESSAGE_SIZE = 4096;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String NEXT_PROBLEM = "Следующее задание";
    private static final String NEXT_PROBLEM_SPECIAL = "Продолжить";
    private static final String FINISH_LESSON = "Завершить урок";
    private static final String CHECK = "Проверить";

    private static final String LESSONS_CMD = "Уроки";

    private static final String START_LESSON = "Начать урок";
    private static final String LESSON_STAT_CMD = "Статистика урока";
    private static final String LESSON_DESC_CMD = "Описание урока";
    private static final String BACK_TO_LESSONS_CMD = "К урокам";

    private static final String SEVERAL_DAYS_STAT_CMD = "Статистика за 7 дней";

    private static final String SUPPORT_ALL_USERS_CMD = "/all_users";
    private static final String SUPPORT_USERS_CMD = "/users";
    private static final String SUPPORT_LESSONS_CMD = "/lessons";
    private static final String SUPPORT_SEARCH_CMD = "/search";


    private static final List<String> REACTIONS_ON_CORRECT_ANSWER = ImmutableList.of(
            "Великолепно!", "Просто супер!", "Правильно!", "И это верный ответ!",
            "Вы сегодня просто в ударе!", "Bingo!", "Perfecto!", "+1", "Верно, продолжайте в том же духе!"
    );
    private static final List<String> REACTIONS_ON_FAILED_ANSWER = ImmutableList.of(
            "В другой раз точно повезет!", "Сделайте работу над ошибками!", "Почти получилось...",
            "Посмотрите как нужно", "Не правильно", "Панда негодует", "Не расстраивайтесь", "Плохие новости",
            "Сегодня не повезло", "Старайтесь лучше"
    );

    public static final int REQUIRED_USER_LAST_LOGIN_TIME_IN_DAYS = 60;

    private final Random randomGenerator = new Random();
    private static final SimpleDateFormat moscowDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        moscowDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    private final UserService userService;
    private final LessonService lessonService;
    private final SentenceService sentenceService;
    private final QuestionService questionService;
    private final UserSessionService userSessionService;
    private final ConfigService configService;
    private final UserMessageService userMessageService;
    private final BotMessageService botMessageService;
    private final PandaService pandaService;

    @Autowired
    public RouteService(SentenceService sentenceService, UserService userService, LessonService lessonService,
                        QuestionService questionService, UserSessionService userSessionService,
                        ConfigService configService, UserMessageService userMessageService,
                        BotMessageService botMessageService, PandaService pandaService) {
        this.sentenceService = sentenceService;
        this.userService = userService;
        this.lessonService = lessonService;
        this.questionService = questionService;
        this.userSessionService = userSessionService;
        this.pandaService = pandaService;
        this.configService = configService;
        this.userMessageService = userMessageService;
        this.botMessageService = botMessageService;
    }

    public void route(int botId, Update update) {
        try {
            process(botId, update);
        } catch (Exception e) {
            log.error("Processing failed", e);
            e.printStackTrace();
        }
    }

    private void process(int botId, Update update) {
        boolean hasMessage = update.hasMessage();
        User user;

        if (hasMessage) {
            user = userService.getUser(update.getMessage().getFrom());

            if (update.getMessage().hasEntities()) {
                processCommands(botId, update.getMessage(), update.getMessage().getEntities(), user);
            } else if (update.getMessage().hasText()) {
                processCommand(botId, update.getMessage(), update.getMessage().getText(), user);
            }
        }
    }

    private void logUserMessage(User user, String userLastMessage, String newMessage) {
        log.info("User: '{} {} [{}]' - LastMessage: '{}', NewMessage: '{}'",
                user.getLastName(), user.getFirstName(), user.getNickName(), userLastMessage, newMessage);

    }

    private void processCommands(int botId, org.telegram.telegrambots.meta.api.objects.Message message, List<MessageEntity> entities, User user) {
        for (MessageEntity e : entities) {
            processCommand(botId, message, e.getText(), user);
        }
    }

    private void processCommand(int botId, org.telegram.telegrambots.meta.api.objects.Message message, String newMessage, User user) {
        Long chatId = message.getChatId();
        Optional<UserSession> oUserSession = userSessionService.getActiveSession(user.getId());
        String lastMessage = userMessageService.getLastMessage(user.getId());

        logUserMessage(user, lastMessage, newMessage);

        if (oUserSession.isPresent()) {
            /* User has studied an active lesson */
            processActiveUserSession(botId, message, newMessage, user, chatId, oUserSession.get());
        } else if (shouldShowLessons(newMessage, lastMessage)) {
            processOpenLesson(user, chatId, botId, newMessage);
        } else if (shouldShowUserSupport(user, newMessage, lastMessage)) {
            processUserSupport(user, chatId, botId, newMessage);
        } else if (shouldSearchPhraseSupport(user, newMessage, lastMessage)) {
            processSearchSupport(chatId, botId, newMessage);
        } else {
            /* First user access or after finish lesson */
            processStartOrReturnToStartPoint(botId, newMessage, user, chatId);
        }

        userMessageService.save(newMessage, user.getId());
        userService.saveUser(user);
    }

    private boolean shouldSearchPhraseSupport(User user, String newMessage, String lastMessage) {
        return user.isAdmin() && (
                (SUPPORT_SEARCH_CMD.equals(lastMessage) && !SUPPORT_SEARCH_CMD.equals(newMessage))
        );
    }

    private boolean shouldShowUserSupport(User user, String newMessage, String lastMessage) {
        return user.isAdmin() && (
                (SUPPORT_ALL_USERS_CMD.equals(lastMessage) && !SUPPORT_ALL_USERS_CMD.equals(newMessage))
                        || (SUPPORT_USERS_CMD.equals(lastMessage) && !SUPPORT_USERS_CMD.equals(newMessage))
        );
    }

    private boolean shouldShowLessons(String newMessage, String userLastMessage) {
        return (LESSONS_CMD.equals(userLastMessage) && !LESSONS_CMD.equals(newMessage)) ||
                (BACK_TO_LESSONS_CMD.equals(userLastMessage) && !BACK_TO_LESSONS_CMD.equals(newMessage));
    }

    private void processStartOrReturnToStartPoint(int botId, String newMessage, User user, Long chatId) {
        switch (newMessage) {
            case LESSONS_CMD:
                if (hasLessons(user.getId())) {
                    botMessageService.sendMessage(botId, chatId,
                            configService.getCommandDescription(LESSONS_CMD + " (Когда у пользователя есть назначенные уроки)", "Посмотри сколько у тебя уроков!\nДавай учиться!"),
                            getLessonsKeyBoard(user.getId())
                    );
                    break;
                }
                botMessageService.sendMessage(
                        botId,
                        chatId,
                        configService.getCommandDescription(
                                LESSONS_CMD + " (Когда у пользователя нет назначеных уроков)",
                                "Кажется, у тебя ещё нет уроков. Спроси учителя об этом."
                        ),
                        true,
                        getStartKeyBoard(user),
                        getSupportKeyBoard(user)
                );
                break;
            case SEVERAL_DAYS_STAT_CMD:
                botMessageService.sendMessage(botId, chatId, getUserStatsForLastWeek(user), true, getStartKeyBoard(user), getSupportKeyBoard(user));
                break;
            case SUPPORT_ALL_USERS_CMD:
            case SUPPORT_USERS_CMD:
                if (user.isAdmin()) {
                    //Show if user active last n days; 0 - show all users
                    int days = newMessage.equals(SUPPORT_ALL_USERS_CMD) ? 0 : REQUIRED_USER_LAST_LOGIN_TIME_IN_DAYS;
                    String prefix = "\nПоказаны все пользователи\nу которых есть назначенные уроки\n";
                    String extra = (days == 0)
                            ? prefix
                            : prefix + "c активностью за последние " + REQUIRED_USER_LAST_LOGIN_TIME_IN_DAYS + " дней";
                    botMessageService.sendMessage(botId, chatId, "Выберите пользователя" + extra, getUsersForKeyBoard(days));
                    break;
                }
            case SUPPORT_SEARCH_CMD:
                if (user.isAdmin()) {
                    botMessageService.sendMessage(botId, chatId, "Введите фразу, например 'angry at', чтобы узнать сколько раз она встречается в предложениях");
                    break;
                }
            case SUPPORT_LESSONS_CMD:
                if (user.isAdmin()) {
                    String lessonsInfo = "*Информация по урокам*\n" + getLessonsInfo();

                    boolean allSent = false;
                    String messageToSend;
                    String restMessage = lessonsInfo;

                    while (!allSent) {
                        if (restMessage.length() > MAX_MESSAGE_SIZE) {
                            messageToSend = restMessage.substring(0, MAX_MESSAGE_SIZE);
                            restMessage = restMessage.substring(MAX_MESSAGE_SIZE);
                        } else {
                            messageToSend = restMessage;
                            allSent = true;
                        }

                        botMessageService.sendMessage(botId, chatId, messageToSend, true, getStartKeyBoard(user), getSupportKeyBoard(user));
                    }
                    break;
                }
            default:
                botMessageService.sendMessage(botId, chatId, configService.getCommandDescription("Приветствие", "Рад видеть, давай учиться!"),
                        true, getStartKeyBoard(user), getSupportKeyBoard(user));
        }
    }

    private void processActiveUserSession(int botId, org.telegram.telegrambots.meta.api.objects.Message message, String newMessage, User user, Long chatId, UserSession userSession) {
        switch (newMessage) {
            case NEXT_PROBLEM_SPECIAL:
                /* To special button marks answer as correct one: Продолжить = Продолжить */
                userSession.process(newMessage);
            case START_LESSON:
            case NEXT_PROBLEM:
                userSession.finishSession();
                processChosenLesson(user, chatId, botId, userSession);
                break;
            case LESSON_DESC_CMD:
                botMessageService.sendMessage(botId, chatId, getLessonDesc(userSession));
                break;
            case LESSON_STAT_CMD:
                botMessageService.sendMessage(botId, chatId, getLessonStats(userSession));
                break;
            case FINISH_LESSON:
                userSession.finishSession();
                botMessageService.sendMessage(botId, chatId,
                        configService.getCommandDescription(FINISH_LESSON, "Возвращайся поскорее!"),
                        true,
                        getStartKeyBoard(user),
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

                String correctAnswer = removeSpacesByRules(userSession.getCorrectQuestion());
                String userAnswer = removeSpacesByRules(userSession.getUserQuestion());

                String checkMessage = "";

                if (userSession.isCorrect()) {
                    checkMessage = correctDesc;
                } else {
                    if (userSession.isMemoryTask()) {
                        Integer memoryMistakesCount = userSession.getMemoryMistakesCount();
                        String memoryMistakeMessage = "";

                        if (memoryMistakesCount != null && memoryMistakesCount > 3) {
                            memoryMistakeMessage = String.format(
                                    "Вы неправильно ответили всего несколько раз: (%s), к сожалению это больше > 3\n\n",
                                    memoryMistakesCount
                            );
                        }

                        checkMessage = memoryMistakeMessage;
                        correctAnswer = userSession.decorate(correctAnswer);
                    }

                    String incorrectMessage = incorrectDesc + "\n\n" +
                            incorrectDesc1 + " " + correctAnswer + "\n" +
                            incorrectDesc2 + " " + userAnswer;

                    checkMessage += incorrectMessage;
                }


                botMessageService.sendMessage(botId, chatId, checkMessage, getFinishKeyBoard());
                sendPanda(botId, chatId, user.getId(), userSession);
                break;
            case BACK_TO_LESSONS_CMD:
                userSession.finishSession();
                botMessageService.sendMessage(botId, chatId,
                        configService.getCommandDescription(LESSONS_CMD + " (Когда у пользователя есть назначенные уроки)", "Посмотри сколько у тебя уроков!\nДавай учиться!"),
                        getLessonsKeyBoard(user.getId())
                );
                break;
            default:
                processUserInput(botId, message, newMessage, chatId, userSession);

        }
        userSessionService.save(userSession);
    }

    private void processUserInput(int botId, org.telegram.telegrambots.meta.api.objects.Message message,
                                  String newMessage, Long chatId, UserSession userSession) {

        botMessageService.deleteMessage(botId, chatId, message.getMessageId());

        boolean isMemory = userSession.isMemoryTask();
        boolean successfulProcessed;
        if (isMemory) {
            successfulProcessed = userSession.processMemory(newMessage);
        } else {
            successfulProcessed = userSession.process(newMessage);
        }

        if (successfulProcessed) {
            if (userSession.hasBotReplyMessageId()) {
                String userInput = isMemory ? userSession.getUserQuestion() : removeSpacesByRules(userSession.getUserQuestion());
                botMessageService.editMessage(botId, chatId, userSession.getBotReplyMessageId(), userInput);
            } else {
                Optional<org.telegram.telegrambots.meta.api.objects.Message> oSentMessage = botMessageService.sendMessage(
                        botId, chatId, userSession.getUserQuestion()
                );
                if (oSentMessage.isPresent()) {
                    org.telegram.telegrambots.meta.api.objects.Message sentMessage = oSentMessage.get();
                    userSession.setBotReplyMessageId(sentMessage.getMessageId());
                }
            }
        }
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
            return "No lesson - no statistic";
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
                .append(" - ").append(100 * userSuccessfulAnsweredQuestions.size() / questions.size()).append("%")
                .append("*").append(ln);
        return statistic.toString();
    }

    private void processSearchSupport(Long chatId, int botId, String newMessage) {
        Collection<LessonToPhrase> lessonsWithPhraseOccurrences = sentenceService.getLessonsWithPhraseOccurrences(newMessage);
        int total = lessonsWithPhraseOccurrences.stream()
                .mapToInt(LessonToPhrase::getOccurrenceCount)
                .sum();

        int maxLessonNameLength = lessonsWithPhraseOccurrences.stream()
                .mapToInt(p -> p.getLessonName().length())
                .max().orElse(0);

        int resizeLen = 5;

        String response = "```\n";
        response += lessonsWithPhraseOccurrences.stream()
                .map(
                        p -> resizeTail(p.getLessonName() + ": ", maxLessonNameLength) + resize(p.getOccurrenceCount(), resizeLen)
                )
                .collect(Collectors.joining("\n"));
        response += "\n\n";
        response += resizeTail("Total:", maxLessonNameLength) + resize(total, resizeLen + 2);
        response += "\n```";
        botMessageService.sendMessage(botId, chatId, response);

    }

    //user firstname lastname\nuser_id
    //chosen user expected - to show stats and lastLogin
    private void processUserSupport(User user, Long chatId, int botId, String newMessage) {
        if (!newMessage.contains("\n")) {
            botMessageService.sendMessage(botId, chatId, "Выберите пользователя", getUsersForKeyBoard(0));
            return;
        }
        String chosenUserId = newMessage.split("\n")[1];
        log.info("UserId: '{}'", chosenUserId);

        int id;
        try {
            id = Integer.parseInt(chosenUserId);
        } catch (Exception e) {
            log.error("Can't parse number: {}", chosenUserId);
            return;
        }
        Optional<User> optionalUser = userService.getUserById(id);
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

        botMessageService.sendMessage(botId, chatId, userInfo.toString(), false, getStartKeyBoard(user), getSupportKeyBoard(user));
        botMessageService.sendMessage(botId, chatId, getUserStatsForLastWeek(chosenUser), true, getStartKeyBoard(user), getSupportKeyBoard(user));
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
        botMessageService.sendMessage(botId, chatId, configService.getCommandDescription("Выбран урок", "Хороший выбор!"), getLessonKeyBoard());
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

        List<String> specialKeyBoard;
        List<String> userKeyBoardButtons = new ArrayList<>(userSession.getUserKeyBoardButtons());

        if (userKeyBoardButtons.contains(NEXT_PROBLEM_SPECIAL)) {
            specialKeyBoard = getContinueKeyBoard();
            userKeyBoardButtons.remove(NEXT_PROBLEM_SPECIAL);
        } else {
            specialKeyBoard = getCheckKeyBoard();
        }

        botMessageService.sendMessage(botId, chatId, userSession.getQuestion().getHighlightedSentence(), true,
                userKeyBoardButtons, specialKeyBoard, userSession.isMemoryTask() ? 7 : 3);
    }

    private String removeSpacesByRules(String s) {
        s = removeSpaceBeforeSigns(s);
        s = removeSpaceAfterSigns(s);
        return s;
    }

    private String removeSpaceBeforeSigns(String s) {
        String symbols = configService.getCommandDescription(
                "Символы / слова перед которыми будут удаляться пробелы только для визуализации" +
                        " (разделитель ' ; ' с пробелами до и после)",
                ", ; . ; ! ; ? ; :"
        );

        List<String> symbolList = Stream.of(symbols.split(" ; ")).collect(Collectors.toList());

        for (String sym : symbolList) {
            s = s.replace(" " + sym, sym);
        }

        return s;
    }

    private String removeSpaceAfterSigns(String s) {
        String symbols = configService.getCommandDescription(
                "Символы / слова после которых будут удаляться пробелы только для визуализации" +
                        " (разделитель ' ; ' с пробелами до и после)",
                "€ ; ¥ ; £ ; RUR ; ₽"
        );

        List<String> symbolList = Stream.of(symbols.split(" ; ")).collect(Collectors.toList());

        for (String sym : symbolList) {
            s = s.replace(sym + " ", sym);
        }

        return s;
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
        int maxLessonNameLength = userStats.stream().mapToInt(u -> u.getLessonName().length()).max().orElse(0);
        int maxResizeLen = Math.max(maxLessonNameLength, resizeLen) + 2;

        StringBuilder sb = new StringBuilder("Статистика за " + periodInDays + " дней\n" +
                "Для лучшего восприятия переверните телефон\n" +
                "Кол-во пройденных заданий" +
                "\n'+' - Done correctly" +
                "\n'-' - Failed" +
                "\n'T' - Total\n\n")

                .append("```\n").append(resizeTail("Date:", maxResizeLen))
                .append(resize("+", resizeLen))
                .append(resize("+%", resizeLen))
                .append(resize("-", resizeLen))
                .append(resize("-%", resizeLen))
                .append(resize("T", resizeLen))
                .append("\n");

        Set<Date> showedDates = new HashSet<>();
        userStats.stream()
                .sorted(Comparator.comparing(UserStat::getStatDate))
                .forEach(us -> {
                    int succeedTaskCount = us.getSucceedTaskCount();
                    int failedTaskCount = us.getFailedTaskCount();
                    int totalTaskCount = us.getTotalTaskCount();

                    if (!showedDates.contains(us.getStatDate())) {
                        sb.append(us.getStatDate()).append("\n");
                        showedDates.add(us.getStatDate());
                    }
                    sb.append(resizeTail(" " + us.getLessonName() + ":", maxResizeLen))
                            .append(resize(succeedTaskCount, resizeLen))
                            .append(resize(100 * succeedTaskCount / totalTaskCount, resizeLen - 1)).append("%")
                            .append(resize(failedTaskCount, resizeLen))
                            .append(resize(100 - (100 * succeedTaskCount / totalTaskCount), resizeLen - 1)).append("%")
                            .append(resize(totalTaskCount, resizeLen)).append("\n");
                });

        sb.append("\n")
                .append(resizeTail("Total:", maxResizeLen))
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
        return resize(i, len, true);
    }

    private String resizeTail(String i, int len) {
        return resize(i, len, false);
    }

    private String resize(String i, int len, boolean addToHead) {

        StringBuilder s = new StringBuilder(String.valueOf(i));
        while (s.length() < len) {
            if (addToHead) {
                s.insert(0, " ");
            } else {
                s.append(" ");
            }
        }
        return s.toString();
    }


    private Optional<Question> getRandomQuestion(User user, Lesson lesson) {
        return questionService.getRandomQuestion(user.getId(), lesson.getId());
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
                .filter(u -> !u.getLessons().isEmpty())
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
                progress = "\n " + percent + "%";
            }
        }
        return progress;
    }

    private List<String> getCheckKeyBoard() {
        return Arrays.asList(CHECK, FINISH_LESSON);
    }

    private List<String> getContinueKeyBoard() {
        return Arrays.asList(NEXT_PROBLEM_SPECIAL, FINISH_LESSON);
    }

    private List<String> getFinishKeyBoard() {
        return Arrays.asList(NEXT_PROBLEM, LESSON_STAT_CMD, LESSON_DESC_CMD, FINISH_LESSON);
    }

    private List<String> getStartKeyBoard(User user) {

        if (user.getLessons().isEmpty()) {
            return Collections.singletonList(LESSONS_CMD);
        }
        return Arrays.asList(LESSONS_CMD, SEVERAL_DAYS_STAT_CMD);
    }

    private List<String> getSupportKeyBoard(User user) {
        if (user.isAdmin()) {
            return Arrays.asList(SUPPORT_USERS_CMD, SUPPORT_ALL_USERS_CMD, SUPPORT_LESSONS_CMD, SUPPORT_SEARCH_CMD);
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getLessonKeyBoard() {
        return Arrays.asList(START_LESSON, LESSON_STAT_CMD, LESSON_DESC_CMD, BACK_TO_LESSONS_CMD);
    }

    public void sendPanda(int botId, long chatId, int userId, UserSession userSession) {
        if (shouldSendPanda(userId, userSession)) {
            Optional<Panda> panda = userSession.isCorrect() ? pandaService.getPositivePanda() : pandaService.getNegativePanda();
            panda.ifPresent(p ->
                    botMessageService.sendPhoto(
                            botId,
                            chatId,
                            "Panda",
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
}
