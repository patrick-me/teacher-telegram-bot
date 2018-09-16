package com.patrick.telegram.model;

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */
@Entity
@Table(name = "Sentence")
public class Sentence implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "name")
    private String name;

    @BatchSize(size = 25)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sentence_id")
    private Collection<Question> questions;

    public Sentence() {
    }

    public Sentence(String name) {
        this.name = name;
    }

    public Sentence(String name, Collection<Question> questions) {
        this.name = name;
        this.questions = questions;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Collection<Question> getQuestions() {
        return questions;
    }
}