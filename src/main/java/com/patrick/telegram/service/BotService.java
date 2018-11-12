package com.patrick.telegram.service;

import com.patrick.telegram.TelegramBot;
import com.patrick.telegram.model.Bot;
import com.patrick.telegram.repository.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;

import javax.transaction.Transactional;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Patrick on 16.03.2018.
 */

@Service
@Transactional
public class BotService {

    //TODO make a config
    private static String PROXY_HOST = "u0k12.tgproxy.me" /* proxy host */;
    private static Integer PROXY_PORT = 1080 /* proxy port */;
    private static String PROXY_USER = "telegram" /* proxy user */;
    private static String PROXY_PASSWORD = "telegram" /* proxy password */;

    @Autowired
    private BotRepository botRepository;

    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
    private final Map<Integer, BotSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, TelegramBot> telegramBotMap = new ConcurrentHashMap<>();

    private void register(Bot bot) {
        try {
            TelegramBot telegramBot = new TelegramBot(bot, getProxyBotOptions());
            System.out.println(telegramBot);
            BotSession botSession = telegramBotsApi.registerBot(telegramBot);
            sessions.put(bot.getId(), botSession);
            telegramBotMap.put(bot.getId(), telegramBot);
            bot.setStatus(Bot.Status.CONNECTED);
        } catch (TelegramApiRequestException e) {
            bot.setStatus(Bot.Status.DISCONNECTED);
            System.out.println(e.getMessage());
        }
    }
//TODO Refactor this
    private DefaultBotOptions getProxyBotOptions() {
        // Create the Authenticator that will return auth's parameters for proxy authentication
       /* System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("http.proxyHost", "http://u0k12.tgproxy.me");
        System.setProperty("http.proxyPort", "1080");
        System.setProperty("http.proxyUser", "telegram");
        System.setProperty("http.proxyPassword", "telegram");
        System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1");*/
        //u0k12.tgproxy.me&port=1080&user=telegram&pass=telegram

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(PROXY_USER, PROXY_PASSWORD.toCharArray());
            }
        });

        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        botOptions.setProxyHost(PROXY_HOST);
        botOptions.setProxyPort(PROXY_PORT);
        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);

        return botOptions;
    }

    public <T extends BotApiMethod<Message>> void send(int id, T message) {
        TelegramBot tb = telegramBotMap.get(id);
        if (tb != null) {
            tb.send(message);
        }
    }

    public void send(int id, SendPhoto message) {
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