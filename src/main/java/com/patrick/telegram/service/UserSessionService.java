package com.patrick.telegram.service;

import com.patrick.telegram.model.UserSession;
import com.patrick.telegram.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class UserSessionService {

    private UserSessionRepository userSessionRepository;

    @Autowired
    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }


    public UserSession getActiveSession(int userId) {
        return userSessionRepository.findOneByUserId(userId);
    }

    public void save(UserSession userSession) {
        userSessionRepository.save(userSession);
    }

    public void update(UserSession userSession) {
        userSessionRepository.update(userSession.getId(), userSession.getUserKeyBoard(), userSession.getUserQuestion(), userSession.isFinished());
    }

    /* Number of sessions correct/incorrect in succession */
    public boolean isLastNumberOfSessionsCorrect(boolean lastUserSessionCorrect, int numberOfSessions, int lessonId, int userId) {
        return Optional.ofNullable(
                userSessionRepository.isLastNumberOfSessionsCorrect(lastUserSessionCorrect, numberOfSessions, lessonId, userId)
        ).orElse(Boolean.FALSE);
    }
}
