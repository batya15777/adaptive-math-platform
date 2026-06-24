package com.adaptive.server.service;

import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.SessionTokenRepository;
import com.adaptive.server.utils.Errors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SessionValidationService} — the guard every authenticated
 * request passes through. It checks token presence, validity, expiry and account
 * status, and layers on per-user and admin-only authorization. A silent bug here is
 * a security hole, so each rejection path asserts both the HTTP status and the reason.
 */
class SessionValidationServiceTest {

    private SessionTokenRepository sessionTokenRepository;
    private SessionValidationService service;

    @BeforeEach
    void setUp() {
        sessionTokenRepository = mock(SessionTokenRepository.class);
        service = new SessionValidationService(sessionTokenRepository);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User user(Long id, String role, String accountStatus) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setAccountStatus(accountStatus);
        return u;
    }

    /** A live (future-expiry), active STUDENT token registered in the repository. */
    private SessionToken liveToken(String value, User user) {
        SessionToken token = new SessionToken(value, Instant.now().plusSeconds(3600), user);
        when(sessionTokenRepository.findByToken(value)).thenReturn(Optional.of(token));
        return token;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("validateAndGetUser()")
    class ValidateAndGetUser {

        @Test
        @DisplayName("Null token → 401 with the 'missing token' reason")
        void nullToken_throws401() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAndGetUser(null));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals(Errors.SESSION_TOKEN_MISSING.getMessage(), ex.getReason());
            verifyNoInteractions(sessionTokenRepository);
        }

        @Test
        @DisplayName("Blank token → 401 'missing token'")
        void blankToken_throws401() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAndGetUser("   "));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals(Errors.SESSION_TOKEN_MISSING.getMessage(), ex.getReason());
        }

        @Test
        @DisplayName("Unknown token → 401 'invalid token'")
        void unknownToken_throws401() {
            when(sessionTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAndGetUser("nope"));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals(Errors.SESSION_TOKEN_INVALID.getMessage(), ex.getReason());
        }

        @Test
        @DisplayName("Expired token → 401 'expired' and the token is deleted")
        void expiredToken_throws401_andDeletes() {
            User u = user(1L, "STUDENT", "ACTIVE");
            SessionToken expired = new SessionToken("old", Instant.now().minusSeconds(60), u);
            when(sessionTokenRepository.findByToken("old")).thenReturn(Optional.of(expired));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAndGetUser("old"));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals(Errors.SESSION_TOKEN_EXPIRED.getMessage(), ex.getReason());
            verify(sessionTokenRepository).delete(expired);
        }

        @Test
        @DisplayName("Live token but inactive account → 403 'account inactive'")
        void inactiveAccount_throws403() {
            liveToken("tok", user(1L, "STUDENT", "BLOCKED"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAndGetUser("tok"));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals(Errors.ACCOUNT_INACTIVE.getMessage(), ex.getReason());
        }

        @Test
        @DisplayName("Live, active token → returns the session token")
        void validActiveToken_returnsToken() {
            SessionToken token = liveToken("tok", user(1L, "STUDENT", "ACTIVE"));

            assertSame(token, service.validateAndGetUser("tok"));
            verify(sessionTokenRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("validateTokenForUser()")
    class ValidateTokenForUser {

        @Test
        @DisplayName("Token owner matches the requested user id → returns the token")
        void matchingUser_returnsToken() {
            SessionToken token = liveToken("tok", user(5L, "STUDENT", "ACTIVE"));

            assertSame(token, service.validateTokenForUser("tok", 5L));
        }

        @Test
        @DisplayName("Token owner differs from the requested user id → 403 (no impersonation)")
        void mismatchedUser_throws403() {
            liveToken("tok", user(5L, "STUDENT", "ACTIVE"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateTokenForUser("tok", 6L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals(Errors.ACCESS_DENIED_USER_MISMATCH.getMessage(), ex.getReason());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("validateAdminOnly()")
    class ValidateAdminOnly {

        @Test
        @DisplayName("ADMIN role → returns the token")
        void adminRole_returnsToken() {
            SessionToken token = liveToken("tok", user(1L, "ADMIN", "ACTIVE"));

            assertSame(token, service.validateAdminOnly("tok"));
        }

        @Test
        @DisplayName("Non-admin role → 403 'admin required'")
        void nonAdmin_throws403() {
            liveToken("tok", user(1L, "STUDENT", "ACTIVE"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAdminOnly("tok"));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals(Errors.ACCESS_DENIED_ADMIN_REQUIRED.getMessage(), ex.getReason());
        }
    }
}
