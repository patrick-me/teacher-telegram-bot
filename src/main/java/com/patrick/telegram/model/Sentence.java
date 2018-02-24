package com.patrick.telegram.model;

import javax.persistence.*;
import java.io.Serializable;

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

    public Sentence() {
    }

    public Sentence(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}