package com.adaptive.server.service;

import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.VerifyEmailRequest;
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
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, EmailVerificationRepository emailVerificationRepository, ValidationService validationService, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.validationService = validationService;
        this.emailService = emailService;
    }

    @Transactional
    public BasicResponse register(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            return new BasicResponse(false, Errors.INVALID_REGISTRATION_REQUEST.getMessage());
        }

        if (!validationService.isValidFullName(registerRequest.getFullName())) {
            return new BasicResponse(false, Errors.INVALID_FULL_NAME.getMessage());
        }

        if (!validationService.isValidPassword(registerRequest.getPassword())) {
            return new BasicResponse(false, Errors.INVALID_PASSWORD.getMessage());
        }

        if (!validationService.isValidEmail(registerRequest.getEmail())) {
            return new BasicResponse(false, Errors.INVALID_EMAIL.getMessage());
        }

        if (!validationService.isValidAge(registerRequest.getAge())) {
            return new BasicResponse(false, Errors.INVALID_AGE.getMessage());
        }

        if (!validationService.isValidGender(registerRequest.getGender())) {
            return new BasicResponse(false, Errors.INVALID_GENDER.getMessage());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new BasicResponse(false, Errors.EMAIL_ALREADY_EXISTS.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        String passwordHash = GenerateHash.hashMd5(registerRequest.getFullName(), registerRequest.getPassword());
        emailVerificationRepository.deleteByEmail(registerRequest.getEmail());
        emailVerificationRepository.flush();
        String verificationCode = generateVerificationCode();
        EmailVerification emailVerification = new EmailVerification(
                registerRequest.getEmail(),
                registerRequest.getFullName(),
                passwordHash,
                registerRequest.getAge(),
                registerRequest.getGender(),
                verificationCode,
                now.plusMinutes(10),
                now
        );
        emailVerificationRepository.save(emailVerification);

        if (!emailService.sendVerificationCode(registerRequest.getEmail(), verificationCode)) {
            emailVerificationRepository.delete(emailVerification);
            return new BasicResponse(false, Errors.VERIFICATION_EMAIL_SEND_FAILED.getMessage());
        }

        return new BasicResponse(true, "Verification code sent successfully.");
    }

    @Transactional
    public BasicResponse verify(VerifyEmailRequest verifyEmailRequest) {
        if (verifyEmailRequest == null || verifyEmailRequest.getEmail() == null || verifyEmailRequest.getCode() == null) {
            return new BasicResponse(false, Errors.INVALID_VERIFICATION_CODE.getMessage());
        }

        EmailVerification emailVerification = emailVerificationRepository
                .findByEmail(verifyEmailRequest.getEmail())
                .orElse(null);

        if (emailVerification == null) {
            return new BasicResponse(false, Errors.VERIFICATION_NOT_FOUND.getMessage());
        }

        if (LocalDateTime.now().isAfter(emailVerification.getExpiresAt())) {
            emailVerificationRepository.delete(emailVerification);
            return new BasicResponse(false, Errors.VERIFICATION_CODE_EXPIRED.getMessage());
        }

        if (!emailVerification.getCode().equals(verifyEmailRequest.getCode())) {
            return new BasicResponse(false, Errors.INVALID_VERIFICATION_CODE.getMessage());
        }

        if (userRepository.existsByEmail(emailVerification.getEmail())) {
            emailVerificationRepository.delete(emailVerification);
            return new BasicResponse(false, Errors.EMAIL_ALREADY_EXISTS.getMessage());
        }

        User user = new User(
                emailVerification.getFullName(),
                emailVerification.getPasswordHash(),
                emailVerification.getEmail(),
                emailVerification.getAge(),
                emailVerification.getGender(),
                LocalDateTime.now()
        );

        userRepository.save(user);
        emailVerificationRepository.delete(emailVerification);

        return new BasicResponse(true, "Email verified and user registered successfully.");
    }

    private String generateVerificationCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
