package com.patrick.telegram.service;

import com.patrick.telegram.model.QuestionType;
import com.patrick.telegram.repository.QuestionTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
@Transactional
public class QuestionTypeService {

    @Autowired
    private QuestionTypeRepository questionTypeRepository;

    public Collection<QuestionType> getQuestionTypes() {
        return questionTypeRepository.findAll();
    }

    public void addQuestionType(QuestionType questionType) {
        questionTypeRepository.save(questionType);
    }
}