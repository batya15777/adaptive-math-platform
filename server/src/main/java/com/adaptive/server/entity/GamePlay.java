package com.adaptive.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * One recorded play of a student game (e.g. Galaxy Battle). History only — a game NEVER changes
 * the student's real level progression. Records who played, the topic + level, win/loss, and the
 * stars spent to enter (so the GAME_ENTRY deduction is auditable later).
 */
@Entity
@Table(name = "game_plays")
public class GamePlay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_key", nullable = false)
    private String gameKey;          // e.g. "galaxy-battle"

    @Column(name = "sub_subject_id")
    private Long subSubjectId;

    @Column(name = "level")
    private Integer level;

    @Column(name = "won", nullable = false)
    private boolean won;

    @Column(name = "stars_spent")
    private Integer starsSpent;      // GAME_ENTRY amount

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GamePlay() {
    }

    public GamePlay(User user, String gameKey, Long subSubjectId, Integer level,
                    boolean won, Integer starsSpent, LocalDateTime createdAt) {
        this.user = user;
        this.gameKey = gameKey;
        this.subSubjectId = subSubjectId;
        this.level = level;
        this.won = won;
        this.starsSpent = starsSpent;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getGameKey() { return gameKey; }
    public Long getSubSubjectId() { return subSubjectId; }
    public Integer getLevel() { return level; }
    public boolean isWon() { return won; }
    public Integer getStarsSpent() { return starsSpent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
