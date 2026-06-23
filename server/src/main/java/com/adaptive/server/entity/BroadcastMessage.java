package com.adaptive.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_messages")
public class BroadcastMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public BroadcastMessage() {}

    public BroadcastMessage(String message, LocalDateTime sentAt) {
        this.message = message;
        this.sentAt  = sentAt;
    }

    public Long getId()            { return id; }
    public String getMessage()     { return message; }
    public LocalDateTime getSentAt() { return sentAt; }

    public void setMessage(String message)     { this.message = message; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
