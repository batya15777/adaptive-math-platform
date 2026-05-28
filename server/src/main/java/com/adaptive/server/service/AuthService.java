package com.adaptive.server.responses;

import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.UserRepository;

public class AuthService {
    private final UserRepository userRepository;
    private EmailVerificationRepository emailVerificationRepository;
}
