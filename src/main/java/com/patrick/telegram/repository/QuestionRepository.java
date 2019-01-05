package com.patrick.telegram.repository;

import com.patrick.telegram.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

/**
 * Created by Patrick on 11.02.2018.
 */
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query(value = "select id from (select q.id, coalesce(t.cnt, 0) cnt from question q\n" +
            "         join lesson_question_types lqt on q.question_type_id = lqt.question_types_id and lesson_id = :lesson_id\n" +
            "         left join \n" +
            "             (select question_id id, count(*) cnt \n" +
            "                    from user_session us where us.correct = 't' and lesson_id = :lesson_id and user_id = :user_id\n" +
            "                    group by question_id) t \n" +
            "         on q.id = t.id\n" +
            "    ) ttt where ttt.cnt in \n" +
            "\n" +
            "        (select min(tt.cnt)\n" +
            "        from \n" +
            "            (select q.id, coalesce(t.cnt, 0) cnt from question q\n" +
            "                 join lesson_question_types lqt on q.question_type_id = lqt.question_types_id and lesson_id = :lesson_id\n" +
            "                 left join \n" +
            "                     (select question_id id, count(*) cnt \n" +
            "                            from user_session us where us.correct = 't' and lesson_id = :lesson_id and user_id = :user_id\n" +
            "                            group by question_id) t \n" +
            "                 on q.id = t.id\n" +
            "            ) tt\n" +
            "        )\n" +
            "order by random()\n" +
            "limit 1;"
            , nativeQuery = true)
    int findRandomOneWithMinIndex(@Param("user_id") int userId, @Param("lesson_id") int lessonId);

    @Modifying()
    @Query(value = "delete from question where sentence_id = :s_id", nativeQuery = true)
    void deleteBySentenceId(@Param("s_id") int id);
}
