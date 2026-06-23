package com.adaptive.server.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Owns password hashing and verification.
 *
 * <p>New passwords use BCrypt. The legacy MD5 branch exists only so existing
 * accounts can log in once and be upgraded transparently by {@link AuthService}.</p>
 */
@Service
public class PasswordService {

    private static final String BCRYPT_PREFIX = "$2";
    private static final int BCRYPT_STRENGTH = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String fullName, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return encoder.matches(rawPassword, storedHash);
        }
        return MessageDigest.isEqual(
                legacyMd5(fullName, rawPassword).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    public boolean needsUpgrade(String storedHash) {
        if (!isBcrypt(storedHash)) return true;           // MD5 → always upgrade
        return bcryptCost(storedHash) < BCRYPT_STRENGTH;  // BCrypt < 12 → upgrade
    }

    private boolean isBcrypt(String storedHash) {
        return storedHash != null && storedHash.startsWith(BCRYPT_PREFIX);
    }

    /** Reads the work factor embedded in a BCrypt hash string ($2a$NN$ format). */
    private int bcryptCost(String hash) {
        try {
            return Integer.parseInt(hash.substring(4, 6));
        } catch (Exception e) {
            return 0;
        }
    }

    private String legacyMd5(String fullName, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(((fullName == null ? "" : fullName) + rawPassword)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is unavailable for legacy password migration", exception);
        }
    }
}
