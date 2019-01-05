package com.patrick.telegram.repository;

import com.patrick.telegram.model.Panda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by Patrick on 28.12.2018.
 */
public interface PandaRepository extends JpaRepository<Panda, Integer> {

    @Query(value = "SELECT * FROM panda p WHERE p.positive order by random() limit 1", nativeQuery = true)
    Panda getPositiveRandomPanda();

    @Query(value = "SELECT * FROM panda p WHERE p.positive = false order by random() limit 1", nativeQuery = true)
    Panda getNegativeRandomPanda();
}