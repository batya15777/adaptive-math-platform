package com.adaptive.server.repository;

import com.adaptive.server.entity.BroadcastMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, Long> {

    /** All messages sent after the given timestamp, newest first. */
    List<BroadcastMessage> findBySentAtAfterOrderBySentAtDesc(LocalDateTime after);
}
