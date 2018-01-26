package com.patrick.telegram.model;

import java.io.Serializable;

/**
 * Created by Patrick on 26.01.2018.
 */
public abstract class Lesson implements Serializable {
    private int id;
    private String group;
    private String name;

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
