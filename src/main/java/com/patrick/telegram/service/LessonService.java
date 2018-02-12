package com.patrick.telegram.service;

import com.patrick.telegram.model.QuestionLesson;
import com.patrick.telegram.repository.QuestionLessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
public class LessonService {

    @Autowired
    private QuestionLessonRepository questionLessonRepository;

    public Collection<QuestionLesson> getQuestionLessons() {
        return questionLessonRepository.findAll();
        //new QuestionLesson(1, "group1", "name1", "What the question is?", "Some answer1, Some ansewer2, Nothing", "Nothing");
    }

    public void addQuestionLesson(QuestionLesson lesson) {
        questionLessonRepository.save(lesson);
    }
}
