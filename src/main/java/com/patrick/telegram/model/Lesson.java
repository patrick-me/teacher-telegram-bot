package com.patrick.telegram.model;

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

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
    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "creation_date")
    private Date creationDate;

    @BatchSize(size = 25)
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "questionType_id")
    private Collection<QuestionType> questionTypes;

    public Lesson() {
    }

    public Lesson(String name, String description) {
        this.name = name;
        this.description = description;
        this.creationDate = new Date();
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

    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return "Lesson{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", creationDate=" + creationDate +
                ", questionTypes=" + questionTypes +
                '}';
    }
}