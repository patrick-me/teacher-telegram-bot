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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "date")
    private Date date;

    @Column(name = "processed", columnDefinition = "boolean default false")
    private boolean isProcessed;

    @Column(name = "user_id")
    private int userId;

    public Message() {
    }

    public Message(String name, int userId) {
        this.name = name;
        this.userId = userId;
        this.date = Date.from(Instant.now());
        this.isProcessed = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getUserId() {
        return userId;
    }

    public Instant getDate() {
        return date.toInstant();
    }

    public boolean isProcessed() {
        return isProcessed;
    }
}
