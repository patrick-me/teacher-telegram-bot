package com.patrick.telegram.service;

import com.patrick.telegram.model.Lesson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
public class RouteService {

    private final BotService botService;
    private final UserService userService;
    private final LessonService lessonService;

    @Autowired
    public RouteService(BotService botService, UserService userService, LessonService lessonService) {
        this.botService = botService;
        this.userService = userService;
        this.lessonService = lessonService;
    }


    public void route(int botId, Update update) {

        boolean hasMessage = update.hasMessage();
        com.patrick.telegram.model.User u;

        if (hasMessage) {
            User user = update.getMessage().getFrom();
            u = userService.getUserByTID(user.getId());

            System.out.println(user);
            System.out.println(u);

            if (u == null) {
                com.patrick.telegram.model.User createdUser = new com.patrick.telegram.model.User(user);
                userService.saveUser(createdUser);
                u = createdUser;
            }
            userService.updateLastLogin(u.getId());

            if (update.getMessage().hasEntities()) {
                processCommands(botId, update.getMessage().getChatId(), update.getMessage().getEntities(), u.getId());
            }
        }
    }

    private void processCommands(int botId, Long chatId, List<MessageEntity> entities, int userId) {
        for (MessageEntity e : entities) {
            processCommand(botId, chatId, e, userId);
        }
    }

    private void processCommand(int botId, Long chatId, MessageEntity entity, int userId) {
        switch (entity.getText()) {
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
                        .setText("Start _command_ handled")
                        .enableMarkdown(true);
                setLessonKeyBoard(lessonMessage, userId);
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
                        .setText("Start _*" + entity.getText() + "*_ handled")
                        .enableMarkdown(true);
                botService.send(botId, defaultMessage);
                break;

        }
    }


    private void setKeyBoard(SendMessage sendMessage) {

        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row
        KeyboardRow row = new KeyboardRow();
        // Set each button, you can also use KeyboardButton objects if you need something else than text
        row.add("/start");
        row.add("/lessons");
        row.add("/bot");
        // Add the first row to the keyboard
        keyboard.add(row);
        // Create another keyboard row
        row = new KeyboardRow();
        // Set each button for the second line
        row.add("/test1");
        row.add("/test2");
        row.add("/test3");
        // Add the second row to the keyboard
        keyboard.add(row);
        // Set the keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);
        // Add it to the message
        sendMessage.setReplyMarkup(keyboardMarkup);
    }

    private void setLessonKeyBoard(SendMessage sendMessage, int userId) {
        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row

        int count = 3;
        KeyboardRow row = new KeyboardRow();
        for (Lesson lesson : lessonService.getUserLessons(userId)) {
            if (count-- < 1) {
                keyboard.add(row);
                row = new KeyboardRow();
                count = 2;
            }
            row.add(lesson.getName());
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        // Set the keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);
        // Add it to the message
        sendMessage.setReplyMarkup(keyboardMarkup);
    }
}