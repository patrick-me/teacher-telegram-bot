package com.patrick.telegram.controller;

import com.patrick.telegram.model.QuestionLesson;
import com.patrick.telegram.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Patrick on 25.01.2018.
 */
@RestController()
@RequestMapping("/lessons")
public class LessonController {

    @Autowired
    LessonService lessonService;

    @GetMapping
    public List<QuestionLesson> getLessons() {
        return Arrays.asList(
                lessonService.getSomeQuestionLesson(),
                lessonService.getSomeQuestionLesson()
        );
    }
}
