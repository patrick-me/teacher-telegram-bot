package com.patrick.telegram.repository;

import com.patrick.telegram.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Created by Patrick on 05.09.2018.
 */

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    @Modifying()
    @Query(value = "delete from lesson_question_types where lesson_id = :id", nativeQuery = true)
    void deleteAssignedQuestionTypesByLesson(@Param("id") int id);

    @Modifying()
    @Query(value = "delete from user2_lessons where lessons_id = :id", nativeQuery = true)
    void deleteAssignedUsersByLesson(@Param("id") int id);
}