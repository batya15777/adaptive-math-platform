package com.adaptive.server.repository;

import com.adaptive.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findTop10ByOrderByTotalStarsDesc();

    // Admin analytics — role is a String column on User (default "STUDENT").
    long countByRole(String role);

    // Leaderboard scoped to a role, so admins (even ex-students) don't appear.
    List<User> findTop10ByRoleOrderByTotalStarsDesc(String role);

}
