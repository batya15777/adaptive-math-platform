package com.adaptive.server.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String email;

    private Integer age;

    private String gender;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "total_stars", nullable = false)
    private Integer totalStars = 0;

    // Date the student last claimed the daily-practice bonus. Used server-side to award
    // it at most once per day (nullable so existing rows are valid after ddl-auto adds it).
    @Column(name = "last_daily_bonus_date")
    private LocalDate lastDailyBonusDate;

    @Column(name = "role", nullable = false)
    private String role = "STUDENT"; // ברירת מחדל: כל מי שנרשם הוא תלמיד

    // Account lifecycle status: ACTIVE | BLOCKED (DELETED reserved for future soft-delete).
    // DB default 'ACTIVE' so existing rows stay usable after the column is added (ddl-auto).
    @Column(name = "account_status", nullable = false, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'")
    private String accountStatus = "ACTIVE";

    public User() {
    }

    public User(String fullName, String passwordHash, String email, Integer age, String gender, LocalDateTime createdAt) {
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTotalStars() {
        return totalStars == null ? 0 : totalStars;
    }

    public void setTotalStars(Integer totalStars) {
        this.totalStars = (totalStars == null) ? 0 : totalStars;
    }

    public LocalDate getLastDailyBonusDate() {
        return lastDailyBonusDate;
    }

    public void setLastDailyBonusDate(LocalDate lastDailyBonusDate) {
        this.lastDailyBonusDate = lastDailyBonusDate;
    }

    public String getAccountStatus() {
        return accountStatus == null ? "ACTIVE" : accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
}
