package com.adaptive.server.service;

import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.repository.SessionTokenRepository;
import com.adaptive.server.utils.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
public class SessionValidationService {

    private final SessionTokenRepository sessionTokenRepository;

    public SessionValidationService(SessionTokenRepository sessionTokenRepository) {
        this.sessionTokenRepository = sessionTokenRepository;
    }


    public SessionToken validateTokenForUser(String tokenCookie, Long userId) {
        SessionToken sessionToken = validateAndGetUser(tokenCookie);
        // This is the critical check that prevents one student acting as another.
        if (!sessionToken.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    Errors.ACCESS_DENIED_USER_MISMATCH.getMessage()
            );
        }
        return sessionToken;
    }

    public SessionToken validateAdminOnly(String tokenCookie) {
        SessionToken sessionToken = validateAndGetUser(tokenCookie);
        if (!"ADMIN".equals(sessionToken.getUser().getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    Errors.ACCESS_DENIED_ADMIN_REQUIRED.getMessage()
            );
        }
        return sessionToken;
    }


    public SessionToken validateAndGetUser(String tokenCookie) {
        if (tokenCookie == null || tokenCookie.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    Errors.SESSION_TOKEN_MISSING.getMessage()            );
        }
        Optional<SessionToken> optionalToken = sessionTokenRepository.findByToken(tokenCookie);
        if (!optionalToken.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    Errors.SESSION_TOKEN_INVALID.getMessage()            );
        }
        SessionToken sessionToken = optionalToken.get();
        if (Instant.now().isAfter(sessionToken.getExpiryDate())) {
            sessionTokenRepository.delete(sessionToken);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    Errors.SESSION_TOKEN_EXPIRED.getMessage()
            );
        }
        // Blocked accounts are denied on the next request even with a live session.
        if (!"ACTIVE".equals(sessionToken.getUser().getAccountStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    Errors.ACCOUNT_BLOCKED.getMessage()
            );
        }
        return sessionToken;
    }
}
