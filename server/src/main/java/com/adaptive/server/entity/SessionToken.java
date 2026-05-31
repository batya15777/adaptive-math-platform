package com.adaptive.server.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "session_tokens")
public class SessionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate; //זמן מדויק

    //כאן אני מקשרת את הטוקן אל משתמש ספציפי
    @ManyToOne(fetch = FetchType.EAGER) //יכולים להיות הרבה טוקנים ששיייכים למשתמש אחד
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    public SessionToken() {}

    public SessionToken(String token, Instant expiryDate, User user) {
        this.token = token;
        this.expiryDate = expiryDate;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
