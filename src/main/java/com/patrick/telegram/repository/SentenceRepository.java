package com.patrick.telegram.repository;

import com.patrick.telegram.model.LessonToPhrase;
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

    @Query(value = "select lessonToSentences.lName lessonName, count(*) occurrenceCount from\n" +
            "(select distinct l.name lName, s.name as sName\n" +
            "from lesson l\n" +
            "    join lesson_question_types lqt on lqt.lesson_id = l.id\n" +
            "    join question q on q.question_type_id = lqt.question_types_id\n" +
            "    join sentence s on s.id = q.sentence_id\n" +
            "where lower(s.name) like lower(CONCAT('%',:phrase,'%'))) lessonToSentences\n" +
            "group by lessonName\n" +
            "order by lessonName", nativeQuery = true)
    Collection<LessonToPhrase> getLessonsWithPhraseOccurrences(@Param("phrase") String phrase);
}
