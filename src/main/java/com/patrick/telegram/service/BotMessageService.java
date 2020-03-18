package com.patrick.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class BotMessageService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final BotService botService;

    public BotMessageService(BotService botService) {
        this.botService = botService;
    }

    public Optional<Message> sendMessage(int botId, long chatId, String text) {
        return sendMessage(botId, chatId, text, Collections.emptyList());
    }

    public Optional<Message> sendMessage(int botId, long chatId, String text, List<String> keyBoardButtons) {
        return sendMessage(botId, chatId, text, true, keyBoardButtons, Collections.emptyList());
    }

    public Optional<Message> sendMessage(int botId, long chatId, String text,
                                         boolean enableMarkdown,
                                         List<String> keyBoardButtons,
                                         List<String> keyBoardControlButtons) {
        return sendMessage(botId, chatId, text, enableMarkdown, keyBoardButtons, keyBoardControlButtons, 3);
    }

    public Optional<Message> sendMessage(int botId, long chatId, String text,
                                         boolean enableMarkdown,
                                         List<String> keyBoardButtons,
                                         List<String> keyBoardControlButtons,
                                         int numberOfElementsPerRow) {
        if (StringUtils.isEmpty(text)) {
            text = "There is no text here yet";
        }

        markdownChecks(text, enableMarkdown);

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(enableMarkdown);

        ReplyKeyboardMarkup replyKeyboard = getKeyBoard(keyBoardButtons, keyBoardControlButtons, numberOfElementsPerRow);

        if (!keyBoardButtons.isEmpty() || !keyBoardControlButtons.isEmpty()) {
            message.setReplyMarkup(replyKeyboard);
        }
        return botService.send(botId, message);
    }

    private void markdownChecks(String text, boolean enableMarkdown) {
        Map<Character, Integer> markdownSymbols = new HashMap<>();
        markdownSymbols.put('*', 0);
        markdownSymbols.put('_', 0);

        if (enableMarkdown) {
            text.chars()
                    .forEach(c -> markdownSymbols.computeIfPresent((char) c, (k, v) -> v + 1));

            markdownSymbols.forEach((k, v) -> {
                if (v % 2 == 0) {
                    log.info("Some text contains '{}' '{}' times", k, v);
                } else {
                    log.error("This text '{}'\n contains '{}' '{}' times", text, k, v);
                }
            });
        }
    }

    private ReplyKeyboardMarkup getKeyBoard(List<String> keyboardElements, List<String> keyboardControlElements,
                                            int numberOfElementsPerRow) {
        // Create ReplyKeyboardMarkup object
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row
        addRowsToKeyBoard(keyboard, keyboardElements, numberOfElementsPerRow);
        addRowsToKeyBoard(keyboard, keyboardControlElements, numberOfElementsPerRow);

        // Set the keyboard to the markup
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void addRowsToKeyBoard(List<KeyboardRow> keyboard, List<String> elements,
                                   int numberOfElementsPerRow) {
        int count = numberOfElementsPerRow;
        KeyboardRow row = new KeyboardRow();
        for (String element : elements) {
            if (count-- < 1) {
                keyboard.add(row);
                row = new KeyboardRow();
                count = numberOfElementsPerRow - 1;
            }
            row.add(element);
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }
    }


    public void editMessage(int botId, long chatId, int messageId, String text) {
        EditMessageText message = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .enableMarkdown(true);

        botService.send(botId, message);
    }

    public void deleteMessage(int botId, long chatId, int messageId) {
        DeleteMessage message = new DeleteMessage()
                .setChatId(chatId)
                .setMessageId(messageId);

        botService.send(botId, message);
    }

    //TODO use or remove
    private void findUserPhotos(int botId, int telegramUserId) {
        GetUserProfilePhotos userProfilePhotos = new GetUserProfilePhotos();
        userProfilePhotos = userProfilePhotos.setUserId(telegramUserId).setOffset(0).setLimit(10);

        String token = botService.getBotToken(botId);
        Optional<UserProfilePhotos> send = botService.send(botId, userProfilePhotos);
        System.out.println(send);
        send.ifPresent(p -> {
            GetFile file = new GetFile().setFileId(p.getPhotos().get(0).get(0).getFileId());
            Optional<File> f = botService.send(botId, file);
            f.ifPresent(q -> {
                        System.out.println(q.getFileUrl(token));
                        URL url = null;
                        try {
                            url = new URL(q.getFileUrl(token));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        try {
                            BufferedInputStream bis = new BufferedInputStream(url.openConnection().getInputStream());
                            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(bis);
                            Base64.getEncoder().encode(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );

        });
    }

    public void sendPhoto(int botId, long chatId, String photoName, InputStream is) {
        try {
            SendPhoto message = new SendPhoto()
                    .setChatId(chatId)
                    .setPhoto(photoName, is);
            botService.send(botId, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
