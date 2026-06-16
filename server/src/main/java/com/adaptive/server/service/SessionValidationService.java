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

    /**
     * Validates the session token and asserts it belongs to the given userId.
     *
     * @param tokenCookie  Value of the "session_token" cookie (may be null).
     * @param userId       The userId received in the request body.
     * @return             The resolved SessionToken (safe to use after this call).
     *
     * @throws ResponseStatusException 401 if the token is missing, unknown, or expired.
     * @throws ResponseStatusException 403 if the token does not belong to userId.
     */
    public SessionToken validateTokenForUser(String tokenCookie, Long userId) {
        SessionToken sessionToken = validateAndGetUser(tokenCookie);

        // This is the critical check that prevents one student acting as another.
        if (!sessionToken.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: token does not match the provided user ID."
            );
        }

        return sessionToken;
    }

    /**
     * Validates the session token (existence + expiry) and returns the resolved
     * SessionToken so the caller can extract the authenticated user from it.
     *
     * Use this when the userId is NOT in the request (extracted from the token itself).
     *
     * @param tokenCookie  Value of the "session_token" cookie (may be null).
     * @return             The resolved, non-expired SessionToken.
     *
     * @throws ResponseStatusException 401 if the token is missing, unknown, or expired.
     */
    public SessionToken validateAndGetUser(String tokenCookie) {

        if (tokenCookie == null || tokenCookie.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing session token. Please log in first."
            );
        }

        Optional<SessionToken> optionalToken = sessionTokenRepository.findByToken(tokenCookie);
        if (!optionalToken.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid session token. Please log in again."
            );
        }

        SessionToken sessionToken = optionalToken.get();

        if (Instant.now().isAfter(sessionToken.getExpiryDate())) {
            sessionTokenRepository.delete(sessionToken);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Session has expired. Please log in again."
            );
        }
        return sessionToken;
    }
}
