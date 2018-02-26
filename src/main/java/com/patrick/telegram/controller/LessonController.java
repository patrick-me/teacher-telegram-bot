package com.patrick.telegram.controller;

import com.patrick.telegram.model.Lesson;
import com.patrick.telegram.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Patrick on 25.01.2018.
 */
@RestController()
@RequestMapping("/lessons")
public class LessonController {

    @Autowired
    LessonService lessonService;

    @GetMapping
    public Collection<Lesson> getLessons() {
        return lessonService.getLessons();
    }

    @PostMapping
    public void addLesson(@RequestBody Lesson lesson) {
        lessonService.addLesson(lesson);
    }
}