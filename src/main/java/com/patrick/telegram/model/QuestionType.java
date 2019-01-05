package com.patrick.telegram.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Patrick on 24.02.2018.
 */

@Entity
@Table(name = "QuestionType")
public class QuestionType implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    public QuestionType() {
    }

    public QuestionType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}