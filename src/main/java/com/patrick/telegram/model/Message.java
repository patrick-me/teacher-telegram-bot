package com.patrick.telegram.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

@Entity
@Table(name = Message.TABLE_NAME)
public class Message implements Serializable {
    public static final String TABLE_NAME = "Message";
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "date")
    private Date date;

    public Message() {
    }

    public Message(String name) {
        this.name = name;
        this.date = Date.from(Instant.now());
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getDate() {
        return date.toInstant();
    }
}
