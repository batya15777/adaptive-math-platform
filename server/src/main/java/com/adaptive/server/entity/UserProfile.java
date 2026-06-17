package com.adaptive.server.entity;

import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;

import javax.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme")
    private ProfileTheme theme;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Language language;

    @Column(name = "picture_id")
    private long pictureId;

    public UserProfile() {
    }

    public UserProfile(User user, ProfileTheme theme, Language language, long pictureId) {
        this.user = user;
        this.theme = theme;
        this.language = language;
        this.pictureId = pictureId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ProfileTheme getTheme() {
        return theme;
    }

    public void setTheme(ProfileTheme theme) {
        this.theme = theme;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public long getPictureId() {
        return pictureId;
    }

    public void setPictureId(long pictureId) {
        this.pictureId = pictureId;
    }
}
