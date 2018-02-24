package com.patrick.telegram.repository;

import com.patrick.telegram.model.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;

/**
 * Created by Patrick on 11.02.2018.
 */
@Transactional
public interface SentenceRepository extends JpaRepository<Sentence, Integer> {
}
