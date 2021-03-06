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

    private final LessonService lessonService;

    @Autowired
    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public Collection<Lesson> getLessons() {
        return lessonService.getLessons();
    }

    @PostMapping
    public void addLesson(@RequestBody Lesson lesson) {
        lessonService.addLesson(lesson);
    }

    @GetMapping("/user/{id}")
    public Collection<Lesson> getUserLessons(@PathVariable int id) {
        return lessonService.getUserLessons(id);
    }

    @PostMapping("/user/{id}")
    public void saveUserLessons(@PathVariable int id, @RequestBody Collection<Lesson> lessons) {
        lessonService.saveUserLessons(id, lessons);
    }

    @DeleteMapping("/{id}")
    public void deleteLesson(@PathVariable int id) {
        lessonService.deleteLesson(id);
    }
}