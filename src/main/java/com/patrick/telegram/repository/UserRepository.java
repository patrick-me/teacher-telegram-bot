package com.patrick.telegram.repository;

import com.patrick.telegram.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

/**
 * Created by Patrick on 17.03.2018.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.telegramId=:id")
    User findOneByTelegramId(@Param("id") int telegramId);

    @Modifying()
    @Query("UPDATE User u SET u.lastLogin = :date WHERE u.id=:id")
    void updateLastLogin(@Param("id") int id, @Param("date") Date date);
}