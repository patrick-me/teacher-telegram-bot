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

    @BatchSize(size = 25)
    @ManyToMany
    @JoinColumn(name = "lesson_id")
    private Collection<Lesson> lessons = new ArrayList<>();

    @BatchSize(size = 25)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    //@JoinTable(name = "(select * from " + Message.TABLE_NAME + " order by date desc limit 1000)")
    private Collection<Message> messages = new ArrayList<>();

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

    public Collection<Message> getMessages() {
        return messages;
    }

    public void addMessage(String message) {
        messages.add(new Message(message));
    }


    public String getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return new ArrayList<>(messages).get(messages.size() - 1).getName();
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
                '}';
    }
}