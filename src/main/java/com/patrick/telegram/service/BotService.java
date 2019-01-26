package com.patrick.telegram.service;

import com.patrick.telegram.TelegramBot;
import com.patrick.telegram.model.Bot;
import com.patrick.telegram.repository.BotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
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

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Value("${proxy.user}")
    private String proxyUser;
    @Value("${proxy.password}")
    private String proxyPassword;
    @Value("${proxy.enabled}")
    private boolean isProxyEnabled;


    @Autowired
    private BotRepository botRepository;

    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
    private final Map<Integer, BotSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, TelegramBot> telegramBotMap = new ConcurrentHashMap<>();

    private void register(Bot bot) {
        try {
            log.info("Bot registration - {}", bot.getName());
            TelegramBot telegramBot = new TelegramBot(bot, getProxyBotOptions());
            log.info("telegramBot: {}", telegramBot);

            BotSession botSession = telegramBotsApi.registerBot(telegramBot);
            sessions.put(bot.getId(), botSession);
            telegramBotMap.put(bot.getId(), telegramBot);
            bot.setStatus(Bot.Status.CONNECTED);
        } catch (TelegramApiRequestException e) {
            bot.setStatus(Bot.Status.DISCONNECTED);
            log.info("error: {}", e.getMessage());
        }
    }

    private DefaultBotOptions getProxyBotOptions() {

        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);

        if (isProxyEnabled) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                }
            });

            botOptions.setProxyHost(proxyHost);
            botOptions.setProxyPort(proxyPort);
            botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        }

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
        for (Bot bot : getBots()) {
            disconnect(bot);
        }
    }

    public void disconnect(Bot bot) {
        BotSession botSession = sessions.get(bot.getId());
        if (botSession != null && botSession.isRunning()) {
            botSession.stop();
            sessions.remove(bot.getId());
        }
        bot.setStatus(Bot.Status.DISCONNECTED);
        saveBot(bot);
        log.info("Bot {} is disconnected", bot.getName());
    }
}