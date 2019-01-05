package com.patrick.telegram.service;

import com.patrick.telegram.model.User;
import com.patrick.telegram.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Date;

/**
 * Created by Patrick on 17.03.2018.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Collection<User> getUsers() {
        return userRepository.findAll();
    }

    public User getUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        User user = userRepository.findOneByTelegramId(telegramUser.getId());

        if (user == null) {
            user = new com.patrick.telegram.model.User(telegramUser);
        }
        user.setLastLogin(new Date());
        saveUser(user);
        return user;
    }

    public User getUser(int tid) {
        return userRepository.findOneByTelegramId(tid);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void updateLastLogin(int id) {
        userRepository.updateLastLogin(id, new Date());
    }
}