package com.patrick.telegram.service;

import com.patrick.telegram.model.UserSession;
import com.patrick.telegram.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class UserSessionService {

    private UserSessionRepository userSessionRepository;

    @Autowired
    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }


    public UserSession getSession(int userId) {
        return userSessionRepository.findOneByUserId(userId);
    }

    public void save(UserSession userSession) {
        userSessionRepository.save(userSession);
    }

    public void update(UserSession userSession) {
        userSessionRepository.update(userSession.getId(), userSession.getUserKeyBoard(), userSession.getUserQuestion(), userSession.isFinished());
    }

}
