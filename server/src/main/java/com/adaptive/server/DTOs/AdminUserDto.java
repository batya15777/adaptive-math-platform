package com.adaptive.server.DTOs;

import com.adaptive.server.entity.User;

import java.time.LocalDateTime;

/**
 * A single user row for the admin user-management list.
 * Intentionally excludes passwordHash and any sensitive field.
 */
public class AdminUserDto {
    private final Long id;
    private final String username;
    private final String email;
    private final String role;
    private final Integer age;
    private final String gender;
    private final Integer totalStars;
    private final LocalDateTime createdAt;
    private final String accountStatus;

    public AdminUserDto(User user) {
        this.id = user.getId();
        this.username = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.age = user.getAge();
        this.gender = user.getGender();
        this.totalStars = user.getTotalStars();
        this.createdAt = user.getCreatedAt();
        this.accountStatus = user.getAccountStatus();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }
    public Integer getTotalStars() { return totalStars; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAccountStatus() { return accountStatus; }
}
