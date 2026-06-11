package com.adaptive.server.DTOs;

import com.adaptive.server.entity.User;

public class UserResponseDTO {
    private Long id;
    private String email;
    private String username;
    private String gender;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getFullName();
        this.gender = user.getGender();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getGender() {
        return gender;
    }
}
