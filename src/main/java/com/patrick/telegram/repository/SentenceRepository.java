package com.patrick.telegram.repository;

import com.patrick.telegram.model.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

/**
 * Created by Patrick on 11.02.2018.
 */
@Transactional
public interface SentenceRepository extends JpaRepository<Sentence, Integer> {

    @Modifying()
    @Query(value = "delete from sentence where id = :s_id", nativeQuery = true)
    void deleteSentence(@Param("s_id") int id);
}
