package com.adaptive.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashesNewPasswordsWithBcrypt() {
        String first = passwordService.hash("Secret12!");
        String second = passwordService.hash("Secret12!");

        assertTrue(first.startsWith("$2"));
        assertNotEquals(first, second, "BCrypt must use a different salt for every hash");
        assertTrue(passwordService.matches("Secret12!", "Any Name", first));
        assertFalse(passwordService.matches("wrong", "Any Name", first));
        assertFalse(passwordService.needsUpgrade(first));
    }

    @Test
    void acceptsLegacyMd5UntilTheAccountIsUpgraded() {
        String legacyHash = "4E8EB454595CAE1570E9C4999580938B"; // MD5("jon" + "wow")

        assertTrue(passwordService.matches("wow", "jon", legacyHash));
        assertFalse(passwordService.matches("wrong", "jon", legacyHash));
        assertTrue(passwordService.needsUpgrade(legacyHash));
    }
}
