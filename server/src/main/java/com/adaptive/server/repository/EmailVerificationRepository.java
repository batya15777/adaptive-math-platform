package com.adaptive.server.repository;

import com.adaptive.server.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification,Long> {
    Optional<EmailVerification> findByEmail(String email);

    void deleteByEmail(String email);
}
