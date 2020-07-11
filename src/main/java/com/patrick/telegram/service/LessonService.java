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

    public static final Comparator<Lesson> LESSON_COMPARATOR = Comparator.comparing(Lesson::getName, (x, y) -> {
        /* Урок 1.26 A Супер */
        String[] xParts = x.split(" ");
        String[] yParts = y.split(" ");


        if (xParts.length > 2 && yParts.length >= 2) {
            /* Урок 1.26 / Урок 1.1 */
            return Integer.compare(
                    xParts[0].length() + xParts[1].length(),
                    yParts[0].length() + yParts[1].length()
            );
        }
        return Integer.compare(x.length(), y.length());
    })
            .thenComparing(Lesson::getName, Comparator.naturalOrder());
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Autowired
    public LessonService(LessonRepository lessonRepository, UserRepository userRepository) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    public Collection<Lesson> getLessons() {
        return lessonRepository.findAll().stream()
                .sorted(LESSON_COMPARATOR)
                .collect(Collectors.toList());
    }

    public void addLesson(Lesson lesson) {
        lessonRepository.save(lesson);
    }

    public Collection<Lesson> getUserLessons(int id) {
        Optional<User> optionalUser = this.userRepository.findById(id);

        if (!optionalUser.isPresent()) {
            return new ArrayList<>();
        } else {
            User user = optionalUser.get();
            user.getLessons().size(); /* Already forgot for what. Maybe kinda a lazy load trigger. */
            return user
                    .getLessons()
                    .stream()
                    .sorted(LESSON_COMPARATOR)
                    .collect(Collectors.toList());
        }
    }

    public Optional<Lesson> getLesson(int id) {
        return lessonRepository.findById(id);
    }

    public Optional<Lesson> getUserLessonByName(int userId, String name) {
        return getUserLessons(userId).stream().filter(l -> l.getName().equals(name)).findFirst();
    }

    public void saveUserLessons(int id, Collection<Lesson> lessons) {
        Optional<User> optionalUser = this.userRepository.findById(id);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setLessons(lessons);
            userRepository.save(user);
        }
    }

    public void deleteLesson(int id) {
        lessonRepository.deleteAssignedQuestionTypesByLesson(id);
        lessonRepository.deleteAssignedUsersByLesson(id);
        lessonRepository.deleteById(id);
    }
}