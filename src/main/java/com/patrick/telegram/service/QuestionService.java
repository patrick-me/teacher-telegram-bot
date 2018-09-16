package com.patrick.telegram.service;

import com.patrick.telegram.model.Question;
import com.patrick.telegram.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
@Transactional
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    public Collection<Question> getQuestions() {
        return questionRepository.findAll();
    }

    public void addQuestionType(Question question) {
        questionRepository.save(question);
    }
}