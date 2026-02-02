package com.example.profileservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Development implementation that logs confirmation URLs instead of sending emails.
 * Replace with actual email implementation for production.
 */
@Service
@Slf4j
public class LoggingEmailService implements EmailService {

    @Override
    public void sendConfirmationEmail(String to, String confirmationUrl) {
        log.info("========================================");
        log.info("CONFIRMATION EMAIL");
        log.info("To: {}", to);
        log.info("Click to confirm: {}", confirmationUrl);
        log.info("========================================");
    }
}
