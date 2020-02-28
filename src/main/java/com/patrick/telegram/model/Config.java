package com.patrick.telegram.model;

import javax.persistence.*;

@Entity
@Table(name = "Config")
public class Config {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "value")
    private String value;

    @Column(name = "isCommand", columnDefinition = "boolean default false")
    private boolean command;

    public Config() {
    }

    public Config(String name, String value, boolean isCommand) {
        this.name = name;
        this.value = value;
        this.command = isCommand;
    }

    public boolean isCommand() {
        return command;
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
