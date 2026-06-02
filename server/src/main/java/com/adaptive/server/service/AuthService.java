package com.adaptive.server.service;

import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.entity.EmailVerification;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.utils.Errors;
import com.adaptive.server.utils.GenerateHash;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ValidationService validationService;

    public AuthService(UserRepository userRepository, EmailVerificationRepository emailVerificationRepository, ValidationService validationService) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.validationService = validationService;
    }

    @Transactional
    public BasicResponse register(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            return new BasicResponse(false, Errors.INVALID_REGISTRATION_REQUEST.getMessage());
        }

        if (!validationService.isValidUsername(registerRequest.getUsername())) {
            return new BasicResponse(false, Errors.INVALID_USERNAME.getMessage());
        }

        if (!validationService.isValidPassword(registerRequest.getPassword())) {
            return new BasicResponse(false, Errors.INVALID_PASSWORD.getMessage());
        }

        if (!validationService.isValidEmail(registerRequest.getEmail())) {
            return new BasicResponse(false, Errors.INVALID_EMAIL.getMessage());
        }

        if (!validationService.isValidGender(registerRequest.getGender())) {
            return new BasicResponse(false, Errors.INVALID_GENDER.getMessage());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new BasicResponse(false, Errors.EMAIL_ALREADY_EXISTS.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        String passwordHash = GenerateHash.hashMd5(registerRequest.getUsername(), registerRequest.getPassword());
        User user = new User(
                registerRequest.getUsername(),
                passwordHash,
                registerRequest.getEmail(),
                registerRequest.getGender(),
                false,
                now
        );

        userRepository.save(user);

        emailVerificationRepository.deleteByEmail(registerRequest.getEmail());
        EmailVerification emailVerification = new EmailVerification(
                registerRequest.getEmail(),
                generateVerificationCode(),
                now.plusMinutes(10),
                now
        );
        emailVerificationRepository.save(emailVerification);

        return new BasicResponse(true, "Verification code created successfully.");
    }

    private String generateVerificationCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
