package com.patrick.telegram.repository;

import com.patrick.telegram.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by Patrick on 05.09.2018.
 */

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
}