package com.adaptive.server.service;

import com.adaptive.server.DTOs.LoginRequest;
import com.adaptive.server.DTOs.RegisterRequest;
import com.adaptive.server.DTOs.VerifyEmailRequest;
import com.adaptive.server.entity.EmailVerification;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.EmailVerificationRepository;
import com.adaptive.server.repository.PasswordResetRepository;
import com.adaptive.server.repository.SessionTokenRepository;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.responses.BasicResponse;
import com.adaptive.server.responses.LoginResponse;
import com.adaptive.server.utils.Errors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService} — login, logout and the two-step registration
 * (register → e-mail verify). The pure {@link ValidationService} is used for real so
 * the regex rules are genuinely exercised; everything with a side effect (repositories,
 * e-mail, password hashing) is mocked. {@link PasswordService} is mocked so credential
 * outcomes are deterministic (its own hashing is covered by {@code PasswordServiceTest}).
 */
class AuthServiceTest {

    private EmailService emailService;
    private UserRepository userRepository;
    private EmailVerificationRepository emailVerificationRepository;
    private ValidationService validationService;
    private SessionTokenRepository sessionTokenRepository;
    private PasswordResetRepository passwordResetRepository;
    private PasswordService passwordService;

    private AuthService service;

    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "Secret12!";   // valid: 9 chars from [A-Za-z!0-9@#*]
    private static final String FULL_NAME = "Dalia Hen";
    private static final String STORED_HASH = "storedHash";

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        userRepository = mock(UserRepository.class);
        emailVerificationRepository = mock(EmailVerificationRepository.class);
        validationService = new ValidationService();   // real — exercise the actual regexes
        sessionTokenRepository = mock(SessionTokenRepository.class);
        passwordResetRepository = mock(PasswordResetRepository.class);
        passwordService = mock(PasswordService.class);

        service = new AuthService(emailService, userRepository, emailVerificationRepository,
                validationService, sessionTokenRepository, passwordResetRepository, passwordService);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User activeUser() {
        User u = new User(FULL_NAME, STORED_HASH, EMAIL, 20, "female", LocalDateTime.now());
        u.setId(1L);
        return u; // defaults: role STUDENT, accountStatus ACTIVE
    }

    private LoginRequest validLogin() {
        return new LoginRequest(EMAIL, PASSWORD);
    }

    private RegisterRequest validRegister() {
        return new RegisterRequest(FULL_NAME, PASSWORD, EMAIL, 20, "female");
    }

    private SessionToken captureSavedToken() {
        ArgumentCaptor<SessionToken> captor = ArgumentCaptor.forClass(SessionToken.class);
        verify(sessionTokenRepository).save(captor.capture());
        return captor.getValue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Malformed email → generic invalid-credentials, DB never touched")
        void invalidEmailFormat_returnsInvalidCredentials() {
            LoginResponse resp = service.login(new LoginRequest("not-an-email", PASSWORD));

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_CREDENTIALS.getMessage(), resp.getMessage());
            verifyNoInteractions(userRepository, sessionTokenRepository, passwordService);
        }

        @Test
        @DisplayName("Malformed password → generic invalid-credentials, DB never touched")
        void invalidPasswordFormat_returnsInvalidCredentials() {
            LoginResponse resp = service.login(new LoginRequest(EMAIL, "waaaytoolongpassword"));

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_CREDENTIALS.getMessage(), resp.getMessage());
            verifyNoInteractions(userRepository, sessionTokenRepository);
        }

        @Test
        @DisplayName("Unknown email → generic invalid-credentials (no user enumeration)")
        void userNotFound_returnsInvalidCredentials() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            LoginResponse resp = service.login(validLogin());

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_CREDENTIALS.getMessage(), resp.getMessage());
            verify(sessionTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Wrong password → generic invalid-credentials")
        void wrongPassword_returnsInvalidCredentials() {
            User u = activeUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
            when(passwordService.matches(PASSWORD, FULL_NAME, STORED_HASH)).thenReturn(false);

            LoginResponse resp = service.login(validLogin());

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_CREDENTIALS.getMessage(), resp.getMessage());
            verify(sessionTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Correct password but blocked account → account-inactive, no session issued")
        void inactiveAccount_returnsAccountInactive() {
            User u = activeUser();
            u.setAccountStatus("BLOCKED");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
            when(passwordService.matches(PASSWORD, FULL_NAME, STORED_HASH)).thenReturn(true);
            when(passwordService.needsUpgrade(STORED_HASH)).thenReturn(false);

            LoginResponse resp = service.login(validLogin());

            assertFalse(resp.isSuccess());
            assertEquals(Errors.ACCOUNT_INACTIVE.getMessage(), resp.getMessage());
            verify(sessionTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Valid login → success and a session token is persisted for the user")
        void validLogin_succeeds_andPersistsToken() {
            User u = activeUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
            when(passwordService.matches(PASSWORD, FULL_NAME, STORED_HASH)).thenReturn(true);
            when(passwordService.needsUpgrade(STORED_HASH)).thenReturn(false);

            LoginResponse resp = service.login(validLogin());

            assertTrue(resp.isSuccess());
            assertNotNull(resp.getLoginData());
            SessionToken saved = captureSavedToken();
            assertNotNull(saved.getToken());
            assertSame(u, saved.getUser());
            assertTrue(saved.getExpiryDate().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("'Remember me' → a long (~30-day) session expiry")
        void rememberMe_extendsExpiry() {
            User u = activeUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
            when(passwordService.matches(PASSWORD, FULL_NAME, STORED_HASH)).thenReturn(true);
            when(passwordService.needsUpgrade(STORED_HASH)).thenReturn(false);
            LoginRequest req = validLogin();
            req.setRemember(true);

            Instant before = Instant.now();
            service.login(req);

            assertTrue(captureSavedToken().getExpiryDate().isAfter(before.plus(29, ChronoUnit.DAYS)));
        }

        @Test
        @DisplayName("Legacy hash → password is re-hashed and the user saved on first login")
        void legacyHash_isUpgradedOnLogin() {
            User u = activeUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
            when(passwordService.matches(PASSWORD, FULL_NAME, STORED_HASH)).thenReturn(true);
            when(passwordService.needsUpgrade(STORED_HASH)).thenReturn(true);
            when(passwordService.hash(PASSWORD)).thenReturn("newBcryptHash");

            LoginResponse resp = service.login(validLogin());

            assertTrue(resp.isSuccess());
            assertEquals("newBcryptHash", u.getPasswordHash());
            verify(userRepository).save(u);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Null token → already-logged-out, repository untouched")
        void nullToken_isNoOp() {
            BasicResponse resp = service.logout(null);

            assertTrue(resp.isSuccess());
            assertEquals("Already logged out", resp.getMessage());
            verifyNoInteractions(sessionTokenRepository);
        }

        @Test
        @DisplayName("Empty token → already-logged-out, repository untouched")
        void emptyToken_isNoOp() {
            BasicResponse resp = service.logout("");

            assertTrue(resp.isSuccess());
            verifyNoInteractions(sessionTokenRepository);
        }

        @Test
        @DisplayName("Known token → the session row is deleted")
        void existingToken_isDeleted() {
            SessionToken token = new SessionToken("tok", Instant.now().plusSeconds(60), activeUser());
            when(sessionTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

            BasicResponse resp = service.logout("tok");

            assertTrue(resp.isSuccess());
            verify(sessionTokenRepository).delete(token);
        }

        @Test
        @DisplayName("Unknown token → still succeeds, nothing deleted")
        void unknownToken_stillSucceeds() {
            when(sessionTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

            BasicResponse resp = service.logout("ghost");

            assertTrue(resp.isSuccess());
            verify(sessionTokenRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Null request → invalid-registration error")
        void nullRequest_returnsError() {
            BasicResponse resp = service.register(null);

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_REGISTRATION_REQUEST.getMessage(), resp.getMessage());
        }

        @Test
        @DisplayName("Single-word full name fails the name regex")
        void invalidFullName_returnsError() {
            RegisterRequest req = new RegisterRequest("Dalia", PASSWORD, EMAIL, 20, "female");

            BasicResponse resp = service.register(req);

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_FULL_NAME.getMessage(), resp.getMessage());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Too-short password fails the password regex")
        void invalidPassword_returnsError() {
            RegisterRequest req = new RegisterRequest(FULL_NAME, "x", EMAIL, 20, "female");

            BasicResponse resp = service.register(req);

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_PASSWORD.getMessage(), resp.getMessage());
        }

        @Test
        @DisplayName("Already-registered email → email-exists error, no email sent")
        void duplicateEmail_returnsError() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            BasicResponse resp = service.register(validRegister());

            assertFalse(resp.isSuccess());
            assertEquals(Errors.EMAIL_ALREADY_EXISTS.getMessage(), resp.getMessage());
            verify(emailVerificationRepository, never()).save(any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Valid request → a 6-digit code is stored and emailed")
        void validRegistration_storesAndEmailsCode() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordService.hash(PASSWORD)).thenReturn("hashed");
            when(emailService.sendVerificationCode(eq(EMAIL), anyString())).thenReturn(true);

            BasicResponse resp = service.register(validRegister());

            assertTrue(resp.isSuccess());
            verify(emailVerificationRepository).save(any(EmailVerification.class));
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendVerificationCode(eq(EMAIL), codeCaptor.capture());
            assertTrue(codeCaptor.getValue().matches("\\d{6}"), "verification code should be 6 digits");
        }

        @Test
        @DisplayName("Email send failure → the staged verification is rolled back")
        void emailSendFails_deletesStagedVerification() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordService.hash(PASSWORD)).thenReturn("hashed");
            when(emailService.sendVerificationCode(eq(EMAIL), anyString())).thenReturn(false);

            BasicResponse resp = service.register(validRegister());

            assertFalse(resp.isSuccess());
            assertEquals(Errors.VERIFICATION_EMAIL_SEND_FAILED.getMessage(), resp.getMessage());
            verify(emailVerificationRepository).delete(any(EmailVerification.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("verify() — step 2 of registration, where the User is created")
    class Verify {

        private EmailVerification verification(String code, LocalDateTime expiresAt) {
            return new EmailVerification(EMAIL, FULL_NAME, "hashed", 20, "female",
                    code, expiresAt, LocalDateTime.now());
        }

        @Test
        @DisplayName("Null request → invalid verification code")
        void nullRequest_returnsError() {
            BasicResponse resp = service.verify(null);

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_VERIFICATION_CODE.getMessage(), resp.getMessage());
        }

        @Test
        @DisplayName("Expired code → expired error and the staged record is deleted")
        void expiredCode_returnsExpired() {
            EmailVerification ev = verification("123456", LocalDateTime.now().minusMinutes(1));
            when(emailVerificationRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ev));

            BasicResponse resp = service.verify(new VerifyEmailRequest(EMAIL, "123456"));

            assertFalse(resp.isSuccess());
            assertEquals(Errors.VERIFICATION_CODE_EXPIRED.getMessage(), resp.getMessage());
            verify(emailVerificationRepository).delete(ev);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Wrong code → invalid-code error, no user created")
        void wrongCode_returnsInvalid() {
            EmailVerification ev = verification("123456", LocalDateTime.now().plusMinutes(5));
            when(emailVerificationRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ev));

            BasicResponse resp = service.verify(new VerifyEmailRequest(EMAIL, "000000"));

            assertFalse(resp.isSuccess());
            assertEquals(Errors.INVALID_VERIFICATION_CODE.getMessage(), resp.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Correct code → a STUDENT user is created and the staged record removed")
        void validCode_createsStudentUser() {
            EmailVerification ev = verification("123456", LocalDateTime.now().plusMinutes(5));
            when(emailVerificationRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ev));
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

            BasicResponse resp = service.verify(new VerifyEmailRequest(EMAIL, "123456"));

            assertTrue(resp.isSuccess());
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User created = userCaptor.getValue();
            assertEquals(EMAIL, created.getEmail());
            assertEquals("hashed", created.getPasswordHash());
            assertEquals("STUDENT", created.getRole());   // registration only ever makes students
            verify(emailVerificationRepository).delete(ev);
        }
    }
}
