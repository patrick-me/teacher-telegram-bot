package com.patrick.telegram.repository;

import com.patrick.telegram.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Integer>, CrudRepository<UserSession, Integer> {

    @Query("SELECT u FROM UserSession u WHERE u.finished = false AND u.userId=:id")
    UserSession findOneByUserId(@Param("id") int userId);

    @Modifying()
    @Query("UPDATE UserSession u SET u.userKeyBoard =:userKeyBoard, u.userQuestion =:userQuestion, u.finished =:finished WHERE u.id=:id")
    void update(@Param("id") int id, @Param("userKeyBoard") String userKeyBoard, @Param("userQuestion") String userQuestion, @Param("finished") boolean finished);
}
