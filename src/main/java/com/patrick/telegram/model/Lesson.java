package com.patrick.telegram.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Patrick on 26.01.2018.
 */

@MappedSuperclass
public abstract class Lesson implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "groupname")
    private String group;
    @Column(name = "name")
    private String name;

    public Lesson() {
    }

    public Lesson(int id, String group, String name) {
        this.id = id;
        this.group = group;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }
}
