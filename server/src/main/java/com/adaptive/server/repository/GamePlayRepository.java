package com.adaptive.server.repository;

import com.adaptive.server.entity.GamePlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GamePlayRepository extends JpaRepository<GamePlay, Long> {

    // How many times this student has won a given game — drives the visual "victory" badge.
    long countByUserIdAndGameKeyAndWonTrue(Long userId, String gameKey);
}
