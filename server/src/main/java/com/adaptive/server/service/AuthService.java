package com.adaptive.server.service;

import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.LoginSuccessData;
import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.VerifyEmailRequest;
import com.adaptive.server.entity.EmailVerification;
import com.adaptive.server.DTOs.UserResponseDTO;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.SessionTokenRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.responses.LoginResponse;
import com.adaptive.server.utils.Errors;
import com.adaptive.server.utils.GenerateHash;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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


    public AuthService(EmailService emailService ,UserRepository userRepository, EmailVerificationRepository emailVerificationRepository, ValidationService validationService , SessionTokenRepository sessionTokenRepository) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.validationService = validationService;
        this.sessionTokenRepository = sessionTokenRepository;
        this.emailService = emailService;
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

        String hashedPassword = GenerateHash.hashMd5(user.getFullName() , loginRequest.getPassword());
        if (!hashedPassword.equals(user.getPasswordHash())) {
            return new LoginResponse(false , Errors.INVALID_CREDENTIALS.getMessage() , null);
        }//לוקחים סיסמא שלקוח הקליד נכניס לפונקציית HASH שלנו ונבדוק אם תוצאה שווה למה שיש בDB

        // Blocked accounts cannot log in (admin soft-block via accountStatus).
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return new LoginResponse(false , Errors.ACCOUNT_BLOCKED.getMessage() , null);
        }

        //יצרתי ככה סשן שיהיה תקף בינתיים ליום ואז שומרת אותו
        String tokenString = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(1 , ChronoUnit.DAYS);

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
