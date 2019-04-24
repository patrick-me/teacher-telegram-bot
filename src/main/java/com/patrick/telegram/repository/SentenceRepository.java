package com.patrick.telegram.repository;

import com.patrick.telegram.model.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Collection;

/**
 * Created by Patrick on 11.02.2018.
 */
@Transactional
public interface SentenceRepository extends JpaRepository<Sentence, Integer> {

    @Modifying()
    @Query(value = "delete from sentence where id = :s_id", nativeQuery = true)
    void deleteSentence(@Param("s_id") int id);

    @Query(value = "select distinct s.* from question q" +
            "  join question_type qt on qt.id = q.question_type_id" +
            "  join lesson_question_types lqt on lqt.question_types_id = qt.id and lqt.lesson_id = :lessonId" +
            "  join sentence s on s.id = q.sentence_id", nativeQuery = true)
    Collection<Sentence> getSentences(@Param("lessonId") int lessonId);
}
