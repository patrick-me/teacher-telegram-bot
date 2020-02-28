package com.patrick.telegram.service;

import com.patrick.telegram.model.Question;
import com.patrick.telegram.model.QuestionType;
import com.patrick.telegram.repository.QuestionRepository;
import com.patrick.telegram.repository.QuestionTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
@Transactional
public class QuestionService {
    public static final Question QUESTION_STUB = new Question(new QuestionType("stub", "stub"), null, "stub", null);

    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionTypeRepository questionTypeRepository;

    public Collection<Question> getQuestions() {
        return questionRepository.findAll();
    }

    public Optional<Question> getRandomQuestion(int userId, int lessonId) {
        int nextQuestionId = questionRepository.findRandomOneWithMinIndex(userId, lessonId);
        return questionRepository.findById(nextQuestionId);
    }

    public Question getQuestionStub() {
        Question stub = questionRepository.getQuestionStub();
        if (stub == null) {
            addQuestion(QUESTION_STUB);
        }
        return (stub == null) ? questionRepository.getQuestionStub() : stub;
    }

    public void addQuestion(Question question) {
        questionTypeRepository.save(question.getQuestionType());
        questionRepository.save(question);
    }

    public Collection<Question> getQuestions(int lessonId) {
        Collection<Question> questions = questionRepository.getQuestions(lessonId);
        return questions == null ? Collections.emptyList() : questions;
    }

    public Collection<Question> getSuccessfulAnsweredQuestions(int userId, int lessonId) {
        Collection<Question> questions = questionRepository.getSuccessfulAnsweredQuestions(userId, lessonId);
        return questions == null ? Collections.emptyList() : questions;
    }
}