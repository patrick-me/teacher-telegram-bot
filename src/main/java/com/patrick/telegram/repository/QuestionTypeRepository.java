package com.patrick.telegram.repository;

import com.patrick.telegram.model.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;

/**
 * Created by Patrick on 11.02.2018.
 */
@Transactional
public interface QuestionTypeRepository extends JpaRepository<QuestionType, Integer> {
}
