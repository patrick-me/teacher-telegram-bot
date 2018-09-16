package com.patrick.telegram.service;

import com.patrick.telegram.model.Lesson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
public class RouteService {

    private final BotService botService;
    private final UserService userService;
    private final LessonService lessonService;
    private final QuestionService questionService;

    @Autowired
    public RouteService(BotService botService, UserService userService, LessonService lessonService,
                        QuestionService questionService) {
        this.botService = botService;
        this.userService = userService;
        this.lessonService = lessonService;
        this.questionService = questionService;
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

        if ("/lessons".equals(user.getLastMessage())) {
            processChosenLesson(user, chatId, botId, newMessage);
        } else {

            switch (newMessage) {
                case "/start":
                    SendMessage message = new SendMessage()
                            .setChatId(chatId)
                            .setText("Start _command_ handled")
                            .enableMarkdown(true);
                    setKeyBoard(message);
                    botService.send(botId, message);
                    break;
                case "/lessons":
                    SendMessage lessonMessage = new SendMessage()
                            .setChatId(chatId)
                            .setText("Lesson _command_ handled")
                            .enableMarkdown(true);
                    setLessonKeyBoard(lessonMessage, user.getId());
                    botService.send(botId, lessonMessage);
                    break;
                case "/test1":
                    SendMessage mes = new SendMessage()
                            .setChatId(chatId)
                            .setText(new String(Character.toChars(128515)) + "\u27a1" + "\u27a2" + "\u27a3" + "\u27a4" + "\u27a5" + "\u27a6" + "\u27a7" + " His younger *BROTHER* is in love with her older sister. → ")
                            .enableMarkdown(true);
                    botService.send(botId, mes);
                    break;

                default:
                    SendMessage defaultMessage = new SendMessage()
                            .setChatId(chatId)
                            .setText("Start _*" + newMessage + "*_ handled")
                            .enableMarkdown(true);
                    botService.send(botId, defaultMessage);
                    break;
            }
        }

        user.addMessage(newMessage);
        userService.saveUser(user);
    }

    private void processChosenLesson(com.patrick.telegram.model.User user, Long chatId, int botId, String newMessage) {
        Collection<Lesson> lessons = lessonService.getUserLessons(user.getId());
        Optional<Lesson> oLesson = lessons.stream().filter(l -> l.getName().equals(newMessage)).findFirst();
        if (!oLesson.isPresent()) {
            return;
        }

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(
                        oLesson.get().getDescription()
                )
                .enableMarkdown(true);

        botService.send(botId, message);

        oLesson.get().getQuestionTypes().stream().findFirst().ifPresent(q ->
                questionService.getQuestions().stream()
                        .filter(qs -> qs.getQuestionType().getId() == q.getId()).findFirst().ifPresent(
                        qqs -> {
                            SendMessage message2 = new SendMessage()
                                    .setChatId(chatId)
                                    .enableMarkdown(true)
                                    .setText(System.lineSeparator() +
                                            qqs.getHighlightedSentence())
                                    .setReplyMarkup(getKeyBoard(Arrays.asList(qqs.getKeyboard().split(" ; "))));

                            botService.send(botId, message2);
                        })
        );
    }

    private ReplyKeyboard getKeyBoard(List<String> keyboardElements) {
        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row

        int count = 3;
        KeyboardRow row = new KeyboardRow();
        for (String element : keyboardElements) {
            if (count-- < 1) {
                keyboard.add(row);
                row = new KeyboardRow();
                count = 2;
            }
            row.add(element);
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        // Set the keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


    private void setKeyBoard(SendMessage sendMessage) {
        sendMessage.setReplyMarkup(
                getKeyBoard(
                        Arrays.asList("/start", "/lessons", "/bot",
                                "/test1", "/test2", "/test3")
                )
        );
    }

    private void setLessonKeyBoard(SendMessage sendMessage, int userId) {
        sendMessage.setReplyMarkup(
                getKeyBoard(lessonService.getUserLessons(userId).stream()
                        .map(Lesson::getName)
                        .collect(Collectors.toList()))
        );
    }
}