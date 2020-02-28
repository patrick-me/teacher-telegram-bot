package com.patrick.telegram.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "Panda")
public class Panda implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "positive")
    private boolean positive;

    @Column(name = "image_base64", columnDefinition = "text", length = 10000)
    private String imageBase64;

    public Panda() {
    }

    public Panda(String name, boolean positive, String imageBase64) {
        this.name = name;
        this.positive = positive;
        this.imageBase64 = imageBase64;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPositive() {
        return positive;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
