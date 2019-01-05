package com.patrick.telegram.model;

import javax.persistence.*;

@Entity
@Table(name = "Config")
public class Config {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "value")
    private String value;

    public Config() {
    }

    public Config(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
