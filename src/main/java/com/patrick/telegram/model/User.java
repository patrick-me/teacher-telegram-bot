package com.patrick.telegram.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by Patrick on 17.03.2018.
 */
@Entity
@Table(name = "User2")
public class User implements Serializable {

    @GenericGenerator(
            name = "userSequenceGenerator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                    @Parameter(name = "sequence_name", value = "USER_SEQUENCE"),
                    @Parameter(name = "initial_value", value = "1"),
                    @Parameter(name = "increment_size", value = "1")
            }
    )
    @Id
    @GeneratedValue(generator = "userSequenceGenerator")
    private int id;
    @Column(name = "firstName")
    private String firstName;
    @Column(name = "lastName")
    private String lastName;
    @Column(name = "nickName")
    private String nickName;
    @Column(name = "telegramId")
    private int telegramId;
    @Column(name = "lastLogin")
    private Date lastLogin;
    @Column(name = "isAdmin", columnDefinition = "boolean default false")
    private boolean isAdmin;

    @BatchSize(size = 25)
    @ManyToMany
    @JoinColumn(name = "lesson_id")
    private Collection<Lesson> lessons = new ArrayList<>();

    public User() {
    }

    public User(org.telegram.telegrambots.meta.api.objects.User user) {
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.nickName = user.getUserName();
        this.telegramId = user.getId();
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public int getTelegramId() {
        return telegramId;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Collection<Lesson> getLessons() {
        return lessons;
    }

    public void setLessons(Collection<Lesson> lessons) {
        this.lessons = lessons;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", telegramId=" + telegramId +
                ", lastLogin=" + lastLogin +
                ", isAdmin=" + isAdmin +
                ", lessons=" + lessons +
                '}';
    }
}