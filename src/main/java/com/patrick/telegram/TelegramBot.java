package com.patrick.telegram;

import com.patrick.telegram.config.SpringContext;
import com.patrick.telegram.model.Bot;
import com.patrick.telegram.service.RouteService;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 * Created by Patrick on 07.02.2018.
 */

public class TelegramBot extends TelegramLongPollingBot {

    private final String BOT_NAME;
    private final String TOKEN;
    private final int ID;

    private RouteService routeService;

    public TelegramBot(Bot bot) {
        super();
        BOT_NAME = bot.getName();
        TOKEN = bot.getToken();
        ID = bot.getId();
    }

    private RouteService getRouteService() {
        if (routeService == null) {
            ApplicationContext ac = SpringContext.getApplicationContext();
            routeService = ac.getBean(RouteService.class);
        }
        return routeService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        getRouteService().route(ID, update);
    }

    public <T extends BotApiMethod<Message>> void send(T message) {
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
}