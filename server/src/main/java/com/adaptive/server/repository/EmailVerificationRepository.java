package com.adaptive.server.repository;

import com.adaptive.server.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification,Long> {
    void deleteByEmail(String email);
}
