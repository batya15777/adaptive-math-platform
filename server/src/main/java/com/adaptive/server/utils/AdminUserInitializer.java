package com.adaptive.server.utils;

import com.adaptive.server.entity.User;
import com.adaptive.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seeds a single ADMIN account so the admin panel — including "Run clustering"
 * ({@code POST /ml/clustering/run}) — is reachable without hand-editing the database.
 *
 * The app ships with no admin (registration always creates a STUDENT), so without this
 * the only way to get an admin is a manual {@code UPDATE users SET role='ADMIN' ...}.
 *
 * <p>Disabled by default — enable once with {@code app.seed-admin=true} (env {@code SEED_ADMIN=true}).
 * Idempotent: does nothing if the configured admin email already exists. Credentials are
 * overridable via {@code app.admin.*} / env so the default password never has to ship to prod.</p>
 */
@Component
@ConditionalOnProperty(name = "app.seed-admin", havingValue = "true")
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;

    @Value("${app.admin.email:admin@adaptive.com}")
    private String adminEmail;
    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;
    @Value("${app.admin.full-name:Admin User}")
    private String adminFullName;

    public AdminUserInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin seed skipped: user '{}' already exists.", adminEmail);
            return;
        }
        User admin = new User(
                adminFullName,
                // Same hashing the login path uses (MD5 of fullName + password), so the
                // seeded credentials authenticate without any special-casing.
                GenerateHash.hashMd5(adminFullName, adminPassword),
                adminEmail,
                null,   // age
                null,   // gender
                LocalDateTime.now());
        admin.setRole("ADMIN");
        admin.setAccountStatus("ACTIVE");
        userRepository.save(admin);
        log.warn("Seeded ADMIN user '{}'. Log in and change the password.", adminEmail);
    }
}
