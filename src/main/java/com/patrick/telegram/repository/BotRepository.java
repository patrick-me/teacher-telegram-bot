package com.patrick.telegram.repository;

import com.patrick.telegram.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;

/**
 * Created by Patrick on 16.03.2018.
 */
@Transactional
public interface BotRepository extends JpaRepository<Bot, Integer> {
}