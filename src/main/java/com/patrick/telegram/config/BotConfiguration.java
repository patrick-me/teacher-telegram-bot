package com.patrick.telegram.config;

import com.patrick.telegram.model.Bot;
import com.patrick.telegram.service.BotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.ApiContextInitializer;

import javax.annotation.PreDestroy;

/**
 * Created by Patrick on 16.03.2018.
 */
@Configuration
@EnableScheduling
public class BotConfiguration {
    public static final int FIXED_DELAY_IN_MILLIS = 5 * 60 * 1000; // 5 min
    public static final int INITIAL_DELAY_IN_MILLIS = 5 * 1000; // 5 sec

    private final BotService botService;
    private static volatile boolean isRegistered = false;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public BotConfiguration(BotService botService) {
        this.botService = botService;
    }

    @Scheduled(fixedDelay = FIXED_DELAY_IN_MILLIS, initialDelay = INITIAL_DELAY_IN_MILLIS)
    protected void startBots() {
        for (Bot bot : botService.getBots()) {
            if (!isRegistered || bot.getStatus() == Bot.Status.DISCONNECTED) {
                ApiContextInitializer.init();
                log.info("Bot [ID:{}, NAME:{}] is connecting to telegram ...", bot.getId(), bot.getName());
                botService.connect(bot.getId());
                isRegistered = true;
            }
            log.info("Heartbeat - Bot [ID:{}, NAME:{}, STATUS:{}]", bot.getId(), bot.getName(), bot.getStatus());
        }
    }

    @PreDestroy
    private void destroy() {
        botService.disconnectAll();
    }
}