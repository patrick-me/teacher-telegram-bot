package com.patrick.telegram.repository;

import com.patrick.telegram.model.UserSession;
import com.patrick.telegram.model.UserStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface UserSessionRepository extends JpaRepository<UserSession, Integer>, CrudRepository<UserSession, Integer> {

    @Modifying()
    @Query(value = "delete from user_session where question_id in (select id from question where sentence_id = :s_id)", nativeQuery = true)
    void deleteBySentenceId(@Param("s_id") int id);

    @Query(value = "SELECT * FROM user_session WHERE finished = false AND user_id=:id ORDER BY start_date desc LIMIT 1", nativeQuery = true)
    UserSession findOneByUserId(@Param("id") int userId);

    @Modifying()
    @Query("UPDATE UserSession u SET u.userKeyBoard =:userKeyBoard, u.userQuestion =:userQuestion, u.finished =:finished WHERE u.id=:id")
    void update(@Param("id") int id, @Param("userKeyBoard") String userKeyBoard, @Param("userQuestion") String userQuestion, @Param("finished") boolean finished);

    /* +1 - because last user session isn't saved yet, but we know the result in lastUserSessionCorrect */
    @Query(value = "select case\n" +
            "    when T2.cnt % :limit = 0 then true\n" +
            "    else false\n" +
            "    end as res\n" +
            "from (\n" +
            "    select greatest(min(n), 0) as cnt\n" +
            "    from (\n" +
            "        select correct, ROW_NUMBER() OVER (ORDER BY start_date desc) as n\n" +
            "        from user_session u\n" +
            "        WHERE u.finished = true\n" +
            "            AND u.user_Id=:id\n" +
            "            AND u.lesson_id=:lesson_id\n" +
            "        order by start_date desc\n" +
            "    ) T\n" +
            "    where correct <> :correct\n" +
            "    group by correct\n" +
            ") T2\n" +
            "limit 1;",
            nativeQuery = true)
    Boolean isLastNumberOfSessionsCorrect(
            @Param("correct") boolean lastUserSessionCorrect,
            @Param("limit") int numberOfSessions,
            @Param("lesson_id") int lessonId,
            @Param("id") int userId
    );


    @Query(value = "select to_date(cast(start_date at time zone 'utc' at time zone 'MSK' as TEXT),'YYYY-MM-DD') as statDate,\n" +
            "               count(*) as totalTaskCount,\n" +
            "               count(*) FILTER (WHERE correct = True) as succeedTaskCount,\n" +
            "               count(*) FILTER (WHERE correct = False) as failedTaskCount\n" +
            "             from user_session us\n" +
            "               join user2 u on u.id = us.user_id\n" +
            "             where user_id=:id\n" +
            "              AND us.user_key_board is not null\n" +
            "              AND us.finished = True\n" +
            "              AND to_date(cast(start_date at time zone 'utc' at time zone 'MSK' as TEXT),'YYYY-MM-DD') > current_date - interval '30 days'\n" +
            "             group by u.id, statDate\n" +
            "            order by u.id asc, statDate desc\n" +
            "            limit :days",
            nativeQuery = true)
    Collection<UserStat> getUserStats(@Param("id") int userId, @Param("days") int limit);
}