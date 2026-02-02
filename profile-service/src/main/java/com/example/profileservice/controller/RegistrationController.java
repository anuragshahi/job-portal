package com.example.profileservice.controller;

import com.example.profileservice.dto.UserRegistrationDTO;
import com.example.profileservice.model.UserProfileEntity;
import com.example.profileservice.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoints for user self-registration.
 * <p>
 * <b>Security Design:</b>
 * <ul>
 *   <li>These endpoints are explicitly permitted in SecurityConfig without JWT validation</li>
 *   <li>Rate limiting should be applied at the gateway/infrastructure level to prevent abuse</li>
 *   <li>Registration creates a disabled profile; user cannot authenticate until email is confirmed</li>
 *   <li>Keycloak user is only created AFTER email confirmation to prevent spam accounts</li>
 *   <li>Password is BCrypt-hashed in PendingRegistration, never stored in plain text</li>
 *   <li>Confirmation tokens are UUID v4 (122 bits of entropy), expire after configurable period</li>
 * </ul>
 * <p>
 * <b>Flow:</b>
 * <ol>
 *   <li>POST /register - Creates disabled profile + pending registration, sends confirmation email</li>
 *   <li>GET /confirm?token=xxx - Validates token, creates Keycloak user, enables profile</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Registers a new user. Creates a disabled profile and sends confirmation email.
     *
     * @param dto Registration data including email, password, and profile information
     * @return Success message with instructions
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO dto) {
        UserProfileEntity profile = registrationService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful. Please check your email to confirm.",
                        "email", profile.getEmail()
                ));
    }

    /**
     * Confirms registration using the token from confirmation email.
     * Creates the Keycloak user and triggers password setup email.
     *
     * @param token Confirmation token from email
     * @return Success message with next steps
     */
    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam String token) {
        UserProfileEntity profile = registrationService.confirm(token);
        return ResponseEntity.ok(Map.of(
                "message", "Email confirmed. Please check your email to set your password.",
                "email", profile.getEmail()
        ));
    }
}