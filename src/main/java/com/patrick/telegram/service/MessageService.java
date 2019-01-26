package com.patrick.telegram.service;

import com.patrick.telegram.model.Message;
import com.patrick.telegram.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

/**
 * Created by Patrick on 26.01.2019.
 */
@Service
@Transactional
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public String getLastMessage(int userId) {
        return Optional.ofNullable(messageRepository.findLastUserMessage(userId))
                .orElse(new Message()).getName();
    }

    public void save(String message, int userId) {
        messageRepository.save(new Message(message, userId));
    }
}