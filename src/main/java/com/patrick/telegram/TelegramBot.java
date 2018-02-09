package com.patrick.telegram;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.List;

/**
 * Created by Patrick on 07.02.2018.
 */
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasEntities()) {
            processCommands(update.getMessage().getEntities(), update.getMessage().getChatId());
        }
    }

    private void processCommands(List<MessageEntity> entities, Long chatId) {
        for (MessageEntity e : entities) {
            processCommand(e, chatId);
        }
    }

    private void processCommand(MessageEntity entity, Long chatId) {
        switch (entity.getText()) {
            case "/start":
                send(new SendMessage()
                        .setChatId(chatId)
                        .setText("Start command handled"));
        }
    }

    private <T extends BotApiMethod<Message>> void send(T message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return TOKEN;
    }

    private static final String BOT_NAME = "Lesson Bot";
    private static final String TOKEN = "";
}