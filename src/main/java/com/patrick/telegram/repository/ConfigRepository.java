package com.patrick.telegram.repository;

import com.patrick.telegram.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Created by Patrick on 31.12.2018.
 */
public interface ConfigRepository extends JpaRepository<Config, Integer> {

    @Query(value = "select c from Config c where c.name = :name")
    Config get(@Param("name") String configName);
}
