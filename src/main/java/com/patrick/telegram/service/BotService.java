package com.patrick.telegram.service;

import com.patrick.telegram.TelegramBot;
import com.patrick.telegram.model.Bot;
import com.patrick.telegram.repository.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.generics.BotSession;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Patrick on 16.03.2018.
 */

@Service
@Transactional
public class BotService {

    @Autowired
    private BotRepository botRepository;

    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
    private final Map<Integer, BotSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, TelegramBot> telegramBotMap = new ConcurrentHashMap<>();

    private void register(Bot bot) {
        try {
            TelegramBot telegramBot = new TelegramBot(bot);
            BotSession botSession = telegramBotsApi.registerBot(telegramBot);
            sessions.put(bot.getId(), botSession);
            telegramBotMap.put(bot.getId(), telegramBot);
            bot.setStatus(Bot.Status.CONNECTED);
        } catch (TelegramApiRequestException e) {
            bot.setStatus(Bot.Status.DISCONNECTED);
            System.out.println(e.getMessage());
        }
    }

    public <T extends BotApiMethod<Message>> void send(int id, T message) {
        TelegramBot tb = telegramBotMap.get(id);
        if (tb != null) {
            tb.send(message);
        }
    }

    public Collection<Bot> getBots() {
        return botRepository.findAll();
    }

    public Bot getBot(int id) {
        Bot bot = botRepository.getOne(id);
        if (bot == null) {
            throw new RuntimeException("Bot is not found by id: " + id);
        }
        return bot;
    }

    public void saveBot(Bot bot) {
        botRepository.save(bot);
    }

    public Bot.Status getBotStatus(int id) {
        BotSession botSession = sessions.get(id);
        if (botSession == null) {
            return Bot.Status.DISCONNECTED;
        }
        return botSession.isRunning() ? Bot.Status.CONNECTED : Bot.Status.DISCONNECTED;
    }

    public Bot.Status connect(int id) {
        Bot bot = getBot(id);
        register(bot);
        saveBot(bot);
        return bot.getStatus();
    }

    public void disconnectAll() {
        BotSession botSession;
        for (Bot bot : getBots()) {
            botSession = sessions.get(bot.getId());
            if (botSession != null && botSession.isRunning()) {
                botSession.stop();
                sessions.remove(bot.getId());
                bot.setStatus(Bot.Status.DISCONNECTED);
                saveBot(bot);
            }
        }
    }
}