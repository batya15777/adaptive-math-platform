package com.adaptive.server.service;

import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.LoginSuccessData;
import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.UserResponseDTO;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.SessionTokenRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.utils.Errors;
import com.adaptive.server.utils.GenerateHash;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ValidationService validationService;
    private final SessionTokenRepository sessionTokenRepository;


    public AuthService(UserRepository userRepository, EmailVerificationRepository emailVerificationRepository, ValidationService validationService , SessionTokenRepository sessionTokenRepository) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.validationService = validationService;
        this.sessionTokenRepository = sessionTokenRepository;
    }


    public BasicResponse<LoginSuccessData> login(LoginRequest loginRequest) {
        //בדיקת תקינות פלט עם VALIDATION
        if (!validationService.emailRegex(loginRequest.getEmail()) ||
        !validationService.passwordRegex(loginRequest.getPassword())) {
            return new BasicResponse<>(false , Errors.INVALID_CREDENTIALS);
        }

        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());
        if (!optionalUser.isPresent()) {
            return new BasicResponse<>(false , Errors.INVALID_CREDENTIALS);
        }//אם אימייל שלקוח הקליד לא קיים נחזיר שגיאה כללית
        User user = optionalUser.get();

        String hashedPassword = GenerateHash.hashMd5(user.getUsername() , loginRequest.getPassword());
        if (!hashedPassword.equals(user.getPassword())) {
            return new BasicResponse<>(false , Errors.INVALID_CREDENTIALS);
        }//לוקחים סיסמא שלקוח הקליד נכניס לפונקציית HASH שלנו ונבדוק אם תוצאה שווה למה שיש בDB

        //יצרתי ככה סשן שיהיה תקף בינתיים ליום ואז שומרת אותו
        String tokenString = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(1 , ChronoUnit.DAYS);

        SessionToken sessionToken = new SessionToken(tokenString, expiryDate, user);
        sessionTokenRepository.save(sessionToken);

        //מצנזרת נתונים רגישים אני לא רוצה להציג את הסיסמא
        UserResponseDTO userResponseDTO = new UserResponseDTO(user);

        LoginSuccessData loginSuccessData = new LoginSuccessData(userResponseDTO , tokenString);
        return new BasicResponse<>(loginSuccessData);
    }


    public BasicResponse<String> logout(String tokenString) {
        if (tokenString == null || tokenString.isEmpty()) {//אם אין טוקן בכלל הוא מנותק
            return new BasicResponse<>(true,null);
        }
        Optional<SessionToken> sessionTokenOptional = sessionTokenRepository.findByToken(tokenString);
        if (sessionTokenOptional.isPresent()) {
            sessionTokenRepository.delete(sessionTokenOptional.get());
            //מחפשים סשן בDB אם מצאתי מחקתי
        }
        return new BasicResponse<>("Logged out successfully");
    }

//אני יוסיף שגיאות של מייל וממגדר
    public BasicResponse<String> validationRegex(

            RegisterRequest registerRequest

    ){

        if (!validationService.usernameRegex(
                registerRequest.getUsername()
        )){

            return new BasicResponse<>(
                    false,
                    Errors.INVALID_CREDENTIALS
            );

        }

        else if (!validationService.passwordRegex(
                registerRequest.getPassword()
        )){

            return new BasicResponse<>(
                    false,
                    Errors.INVALID_CREDENTIALS
            );

        }

        else if (!validationService.emailRegex(
                registerRequest.getEmail()
        )){

            return new BasicResponse<>(
                    false,
                    Errors.INVALID_CREDENTIALS
            );

        }

        else if (!validationService.isGender(
                registerRequest.getGender()
        )){

            return new BasicResponse<>(
                    false,
                    Errors.INVALID_CREDENTIALS
            );

        }

        return new BasicResponse<>(
                "Validation success"
        );

    }
}


