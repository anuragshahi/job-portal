package com.example.profileservice.service;

import com.example.profileservice.dto.UserRegistrationDTO;
import com.example.profileservice.model.ConfirmationType;
import com.example.profileservice.model.PendingRegistrationEntity;
import com.example.profileservice.model.UserProfileEntity;
import com.example.profileservice.repository.PendingRegistrationEntityRepository;
import com.example.profileservice.repository.UserProfileEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Handles user self-registration flow with email confirmation.
 * <p>
 * <b>Registration Flow:</b>
 * <ol>
 *   <li>User submits registration form (email, password, profile data)</li>
 *   <li>{@link #register} creates disabled UserProfileEntity + PendingRegistrationEntity with BCrypt-hashed password</li>
 *   <li>Confirmation email sent with unique token (UUID v4)</li>
 *   <li>User clicks confirmation link</li>
 *   <li>{@link #confirm} validates token expiry, creates Keycloak user via admin API</li>
 *   <li>Keycloak sends "Set Password" email (UPDATE_PASSWORD required action)</li>
 *   <li>UserProfileEntity enabled, PendingRegistrationEntity deleted</li>
 * </ol>
 * <p>
 * <b>Security Design Decisions:</b>
 * <ul>
 *   <li><b>Keycloak user created AFTER confirmation:</b> Prevents spam/bot accounts in IdP.
 *       Unconfirmed users exist only in our DB, not in Keycloak.</li>
 *   <li><b>Password via Keycloak email:</b> User sets password directly with Keycloak.
 *       Our service never sees the final password - only temporary BCrypt hash during registration.</li>
 *   <li><b>Profile disabled until confirmed:</b> Even if someone bypasses email verification,
 *       the profile.enabled=false flag can be checked in business logic.</li>
 *   <li><b>Token expiry:</b> Configurable via app.registration.token-expiry-hours (default 24h).
 *       Expired tokens are rejected; users must re-register.</li>
 * </ul>
 * <p>
 * <b>TODO for production:</b>
 * <ul>
 *   <li>Implement cleanup job to delete expired PendingRegistrations and their disabled profiles</li>
 *   <li>Add rate limiting to prevent registration spam</li>
 *   <li>Consider CAPTCHA integration for public registration endpoint</li>
 * </ul>
 *
 * @see PendingRegistrationEntity
 * @see KeycloakAdminClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserProfileEntityRepository userProfileRepository;
    private final PendingRegistrationEntityRepository pendingRegistrationRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.registration.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.registration.confirmation-base-url}")
    private String confirmationBaseUrl;

    /**
     * Registers a new user by creating a disabled profile and pending registration.
     * Sends confirmation email with unique token.
     *
     * @param dto Registration data
     * @return The created (disabled) user profile
     */
    @Transactional
    public UserProfileEntity register(UserRegistrationDTO dto) {
        // Check if email already exists
        if (userProfileRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        // Create disabled profile (userId will be set after Keycloak user creation)
        UserProfileEntity profile = UserProfileEntity.builder()
                .userId(UUID.randomUUID().toString()) // Temporary, replaced after KC user creation
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .mobileNumber(dto.getMobileNumber())
                .gender(dto.getGender())
                .age(dto.getAge())
                .enabled(false)
                .build();

        profile = userProfileRepository.save(profile);

        // Create pending registration
        String token = UUID.randomUUID().toString();
        PendingRegistrationEntity pending = PendingRegistrationEntity.builder()
                .userProfile(profile)
                .confirmationToken(token)
                .tokenExpiry(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS))
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .confirmationType(ConfirmationType.EMAIL)
                .createdAt(Instant.now())
                .build();

        pendingRegistrationRepository.save(pending);

        // Send confirmation email
        String confirmationUrl = confirmationBaseUrl + "?token=" + token;
        emailService.sendConfirmationEmail(dto.getEmail(), confirmationUrl);

        log.info("User registered with email {}. Confirmation pending.", dto.getEmail());

        return profile;
    }

    /**
     * Confirms registration by validating token, creating Keycloak user,
     * and enabling the profile.
     *
     * @param token Confirmation token from email
     * @return The confirmed and enabled user profile
     */
    @Transactional
    public UserProfileEntity confirm(String token) {
        PendingRegistrationEntity pending = pendingRegistrationRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid confirmation token"));

        // Check expiry
        if (Instant.now().isAfter(pending.getTokenExpiry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation token has expired");
        }

        UserProfileEntity profile = pending.getUserProfile();

        // Create Keycloak user and trigger password email
        try {
            String keycloakUserId = keycloakAdminClient.createUserWithPasswordAction(
                    profile.getEmail(),
                    profile.getEmail(), // username = email
                    profile.getFirstName(),
                    profile.getLastName()
            );

            // Update profile with Keycloak user ID and enable
            profile.setUserId(keycloakUserId);
            profile.setEnabled(true);
            userProfileRepository.save(profile);

            // Delete pending registration
            pendingRegistrationRepository.delete(pending);

            log.info("User {} confirmed and enabled. Keycloak user created: {}", profile.getEmail(), keycloakUserId);

            return profile;
        } catch (Exception e) {
            log.error("Failed to create Keycloak user for {}: {}", profile.getEmail(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete registration");
        }
    }
}
