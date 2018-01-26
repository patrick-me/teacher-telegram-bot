package com.patrick.telegram.service;

import com.patrick.telegram.model.QuestionLesson;
import org.springframework.stereotype.Service;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
public class LessonService {
    public QuestionLesson getSomeQuestionLesson() {
        return new QuestionLesson(1, "group1", "name1", "What the question is?", "Some answer1, Some ansewer2, Nothing", "Nothing");
    }
}
