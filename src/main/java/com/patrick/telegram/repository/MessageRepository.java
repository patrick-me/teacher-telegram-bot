package com.patrick.telegram.repository;

import com.patrick.telegram.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

/**
 * Created by Patrick on 26.01.2019.
 */
@Transactional
public interface MessageRepository extends JpaRepository<Message, Integer> {
    @Query(value = "select * from message where user_id = :user_id order by date desc limit 1", nativeQuery = true)
    Message findLastUserMessage(@Param("user_id") int userId);
}