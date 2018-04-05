package com.patrick.telegram.service;

import com.patrick.telegram.model.User;
import com.patrick.telegram.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Calendar;
import java.util.Collection;

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

    public User getUser(int id) {
        return userRepository.findOne(id);
    }

    public User getUserByTID(int tid) {
        return userRepository.findOneByTelegramId(tid);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void updateLastLogin(int id) {
        userRepository.updateLastLogin(id, Calendar.getInstance().getTime());
    }
}