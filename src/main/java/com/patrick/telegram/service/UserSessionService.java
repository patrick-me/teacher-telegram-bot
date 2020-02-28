package com.patrick.telegram.service;

import com.patrick.telegram.model.UserSession;
import com.patrick.telegram.model.UserStat;
import com.patrick.telegram.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
@Transactional
public class UserSessionService {

    private UserSessionRepository userSessionRepository;

    @Autowired
    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }


    public Optional<UserSession> getActiveSession(int userId) {
        return Optional.ofNullable(userSessionRepository.findOneByUserId(userId));
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

    public Collection<UserStat> getUserStats(int userId, int periodInDays) {
        Collection<UserStat> userStats = userSessionRepository.getUserStats(userId);
        return userStats == null ? Collections.emptyList() : userStats;
    }
}
