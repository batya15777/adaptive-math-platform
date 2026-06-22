package com.adaptive.server.repository;

import com.adaptive.server.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByEmail(String email);

    void deleteByEmail(String email);
}
