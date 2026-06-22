package com.adaptive.server.service;

import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.LoginSuccessData;
import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.VerifyEmailRequest;
import com.adaptive.server.entity.EmailVerification;
import com.adaptive.server.DTOs.UserResponseDTO;
import com.adaptive.server.entity.PasswordReset;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.PasswordResetRepository;
import com.adaptive.server.repository.SessionTokenRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.responses.LoginResponse;
import com.adaptive.server.utils.Errors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ValidationService validationService;
    private final SessionTokenRepository sessionTokenRepository;
    private final EmailService emailService;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordService passwordService;


    public AuthService(EmailService emailService, UserRepository userRepository,
                       EmailVerificationRepository emailVerificationRepository,
                       ValidationService validationService,
                       SessionTokenRepository sessionTokenRepository,
                       PasswordResetRepository passwordResetRepository,
                       PasswordService passwordService) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.validationService = validationService;
        this.sessionTokenRepository = sessionTokenRepository;
        this.emailService = emailService;
        this.passwordResetRepository = passwordResetRepository;
        this.passwordService = passwordService;
    }


    public LoginResponse login(LoginRequest loginRequest) {
        //בדיקת תקינות פלט עם VALIDATION
        if (!validationService.isValidEmail(loginRequest.getEmail()) ||
        !validationService.isValidPassword(loginRequest.getPassword())) {
            return new LoginResponse(false , Errors.INVALID_CREDENTIALS.getMessage() , null);
        }

        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());
        if (!optionalUser.isPresent()) {
            return new LoginResponse(false , Errors.INVALID_CREDENTIALS.getMessage() , null);
        }//אם אימייל שלקוח הקליד לא קיים נחזיר שגיאה כללית
        User user = optionalUser.get();

        if (!passwordService.matches(loginRequest.getPassword(), user.getFullName(), user.getPasswordHash())) {
            return new LoginResponse(false , Errors.INVALID_CREDENTIALS.getMessage() , null);
        }

        // Existing MD5 accounts are upgraded after their first successful login.
        if (passwordService.needsUpgrade(user.getPasswordHash())) {
            user.setPasswordHash(passwordService.hash(loginRequest.getPassword()));
            userRepository.save(user);
        }

        // Non-active accounts (BLOCKED or DELETED) cannot log in.
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return new LoginResponse(false , Errors.ACCOUNT_INACTIVE.getMessage() , null);
        }

        //יצרתי ככה סשן: "זכור אותי" => 30 יום, אחרת יום אחד.
        String tokenString = UUID.randomUUID().toString();
        long sessionDays = loginRequest.isRemember() ? 30 : 1;
        Instant expiryDate = Instant.now().plus(sessionDays , ChronoUnit.DAYS);

        SessionToken sessionToken = new SessionToken(tokenString, expiryDate, user);
        sessionTokenRepository.save(sessionToken);

        //מצנזרת נתונים רגישים אני לא רוצה להציג את הסיסמא
        UserResponseDTO userResponseDTO = new UserResponseDTO(user);
        LoginSuccessData loginSuccessData = new LoginSuccessData(userResponseDTO , tokenString);

        return new LoginResponse(true , "Login successfully" , loginSuccessData);
    }


    public BasicResponse logout(String tokenString) {
        if (tokenString == null || tokenString.isEmpty()) {//אם אין טוקן בכלל הוא מנותק
            return new BasicResponse(true, "Already logged out");
        }
        Optional<SessionToken> sessionTokenOptional = sessionTokenRepository.findByToken(tokenString);
        if (sessionTokenOptional.isPresent()) {
            sessionTokenRepository.delete(sessionTokenOptional.get());
            //מחפשים סשן בDB אם מצאתי מחקתי
        }
        return new BasicResponse(true , "Logged out successfully");
    }

//אני יוסיף שגיאות של מייל וממגדר
//    public BasicResponse<String> validationRegex(){
//        return null;
//    }

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
        String passwordHash = passwordService.hash(registerRequest.getPassword());
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

    // Step 1 of password reset: email a one-time code. Always returns the same generic
    // message so the response can't be used to discover which emails are registered.
    @Transactional
    public BasicResponse forgotPassword(String email) {
        BasicResponse generic = new BasicResponse(true,
                "If an account exists for this email, a reset code has been sent.");

        if (email == null || !validationService.isValidEmail(email)) {
            return generic;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return generic;
        }

        LocalDateTime now = LocalDateTime.now();
        passwordResetRepository.deleteByEmail(email);
        passwordResetRepository.flush();
        String code = generateVerificationCode();
        passwordResetRepository.save(new PasswordReset(email, code, now.plusMinutes(15), now));

        emailService.sendPasswordResetCode(email, code); // best-effort; failures are logged
        return generic;
    }

    // Step 2 of password reset: validate the code and set a new password.
    @Transactional
    public BasicResponse resetPassword(String email, String code, String newPassword) {
        if (!validationService.isValidPassword(newPassword)) {
            return new BasicResponse(false, Errors.INVALID_PASSWORD.getMessage());
        }

        PasswordReset reset = passwordResetRepository.findByEmail(email).orElse(null);
        if (reset == null) {
            return new BasicResponse(false, Errors.RESET_NOT_FOUND.getMessage());
        }
        if (LocalDateTime.now().isAfter(reset.getExpiresAt())) {
            passwordResetRepository.delete(reset);
            return new BasicResponse(false, Errors.RESET_CODE_EXPIRED.getMessage());
        }
        if (!reset.getCode().equals(code)) {
            return new BasicResponse(false, Errors.RESET_CODE_INVALID.getMessage());
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            passwordResetRepository.delete(reset);
            return new BasicResponse(false, Errors.USER_NOT_FOUND.getMessage());
        }

        user.setPasswordHash(passwordService.hash(newPassword));
        userRepository.save(user);
        passwordResetRepository.delete(reset);

        return new BasicResponse(true, "Password has been reset successfully. Please log in.");
    }

    private String generateVerificationCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
