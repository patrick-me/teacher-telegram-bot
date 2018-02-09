package com.patrick.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

@SpringBootApplication
public class TelegramApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramApplication.class, args);
        initTelegramBot();
    }

    public static void initTelegramBot() {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new TelegramBot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}