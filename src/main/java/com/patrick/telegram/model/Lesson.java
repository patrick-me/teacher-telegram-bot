package com.patrick.telegram.model;

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */

@Entity
@Table(name = "Lesson")
public class Lesson implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    @BatchSize(size = 25)
    @ManyToMany
    @JoinColumn(name = "questionType_id")
    private Collection<QuestionType> questionTypes;

    public Lesson() {
    }

    public Lesson(String name, String description) {
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

    public Collection<QuestionType> getQuestionTypes() {
        return questionTypes;
    }
}