package com.adaptive.server.service;

import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.utils.Errors;
import org.springframework.stereotype.Service;

import java.util.Date;


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


