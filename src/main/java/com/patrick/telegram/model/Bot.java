package com.patrick.telegram.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Patrick on 16.03.2018.
 */

@Entity
@Table(name = "Bot")
public class Bot implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "token")
    private String token;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.DISCONNECTED;

    public Bot() {
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        CONNECTED, DISCONNECTED
    }
}