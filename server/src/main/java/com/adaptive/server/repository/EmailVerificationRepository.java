package com.adaptive.server.repository;

import com.adaptive.server.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification,String> {
    boolean existsByEmailAndCode(
            String email,String code);
}
