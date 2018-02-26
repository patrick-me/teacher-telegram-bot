package com.patrick.telegram.service;

import com.patrick.telegram.model.Lesson;
import com.patrick.telegram.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
@Transactional
public class LessonService {

    @Autowired
    private LessonRepository lessonRepository;

    public Collection<Lesson> getLessons() {
        return lessonRepository.findAll();
    }

    public void addLesson(Lesson lesson) {
        lessonRepository.save(lesson);
    }
}