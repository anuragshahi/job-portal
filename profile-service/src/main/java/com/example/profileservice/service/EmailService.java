package com.example.profileservice.service;

/**
 * Service for sending emails.
 * Implement this interface with actual email provider (SMTP, SendGrid, etc.) for production.
 */
public interface EmailService {

    /**
     * Sends a registration confirmation email with the confirmation link.
     *
     * @param to              recipient email address
     * @param confirmationUrl the URL to confirm registration
     */
    void sendConfirmationEmail(String to, String confirmationUrl);
}
