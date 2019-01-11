package com.patrick.telegram.service;

import com.patrick.telegram.model.Lesson;
import com.patrick.telegram.model.User;
import com.patrick.telegram.repository.LessonRepository;
import com.patrick.telegram.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 26.01.2018.
 */
@Service
@Transactional
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Autowired
    public LessonService(LessonRepository lessonRepository, UserRepository userRepository) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    public Collection<Lesson> getLessons() {
        return lessonRepository.findAll();
    }

    public void addLesson(Lesson lesson) {
        lessonRepository.save(lesson);
    }

    public Collection<Lesson> getUserLessons(int id) {
        User user = userRepository.findOne(id);
        if (user == null) {
            return new ArrayList<>();
        } else {
            user.getLessons().size();
            return user.getLessons()
                    .stream()
                    .sorted(Comparator.comparing(Lesson::getName))
                    .collect(Collectors.toList());
        }
    }

    public Optional<Lesson> getLesson(int id) {
        return Optional.ofNullable(lessonRepository.findOne(id));
    }

    public Optional<Lesson> getUserLessonByName(int userId, String name) {
        return getUserLessons(userId).stream().filter(l -> l.getName().equals(name)).findFirst();
    }

    public void saveUserLessons(int id, Collection<Lesson> lessons) {
        User user = userRepository.findOne(id);
        if (user != null) {
            user.setLessons(lessons);
            userRepository.save(user);
        }
    }
}