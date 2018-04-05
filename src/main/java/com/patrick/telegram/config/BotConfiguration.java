package com.patrick.telegram.config;

import com.patrick.telegram.model.Bot;
import com.patrick.telegram.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created by Patrick on 16.03.2018.
 */

@Component
public class BotConfiguration {
    private final BotService botService;

    @Autowired
    public BotConfiguration(BotService botService) {
        this.botService = botService;
    }

    @PostConstruct
    private void init() {
        ApiContextInitializer.init();

        for (Bot bot : botService.getBots()) {
            botService.connect(bot.getId());
        }
    }

    @PreDestroy
    private void destroy() {
        botService.disconnectAll();
    }
}