package com.adaptive.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email}") String fromEmail
    ) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    public boolean sendVerificationCode(String recipientEmail, String verificationCode) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("RESEND_API_KEY is not configured.");
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("from", fromEmail);
        body.put("to", Collections.singletonList(recipientEmail));
        body.put("subject", "Your Adaptive Math verification code");
        body.put("html", buildVerificationEmail(verificationCode));

        try {
            restTemplate.postForEntity(
                    RESEND_EMAILS_URL,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            return true;
        } catch (RestClientException exception) {
            logger.error("Failed to send verification email through Resend.", exception);
            return false;
        }
    }

    private String buildVerificationEmail(String verificationCode) {
        return "<h2>Verify your email</h2>"
                + "<p>Your Adaptive Math verification code is:</p>"
                + "<p style=\"font-size: 28px; font-weight: bold; letter-spacing: 4px;\">"
                + verificationCode
                + "</p>"
                + "<p>This code expires in 10 minutes.</p>";
    }

    public boolean sendPasswordResetCode(String recipientEmail, String resetCode) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("RESEND_API_KEY is not configured.");
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("from", fromEmail);
        body.put("to", Collections.singletonList(recipientEmail));
        body.put("subject", "Your Adaptive Math password reset code");
        body.put("html", buildPasswordResetEmail(resetCode));

        try {
            restTemplate.postForEntity(
                    RESEND_EMAILS_URL,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            return true;
        } catch (RestClientException exception) {
            logger.error("Failed to send password reset email through Resend.", exception);
            return false;
        }
    }

    private String buildPasswordResetEmail(String resetCode) {
        return "<h2>Reset your password</h2>"
                + "<p>Your Adaptive Math password reset code is:</p>"
                + "<p style=\"font-size: 28px; font-weight: bold; letter-spacing: 4px;\">"
                + resetCode
                + "</p>"
                + "<p>This code expires in 15 minutes. If you didn't request this, you can ignore this email.</p>";
    }
}
