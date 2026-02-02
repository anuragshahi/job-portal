package com.example.profileservice.controller;

import com.example.profileservice.dto.UserProfileResponse;
import com.example.profileservice.dto.UserRegistrationDTO;
import com.example.profileservice.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile/admin")
@RequiredArgsConstructor
public class AdminProfileController {

    private final ProfileService profileService;

    /**
     * Creates a user in Keycloak and then creates a corresponding profile in the database.
     * Implements Saga pattern with compensation via ProfileService.
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse createUser(@Valid @RequestBody UserRegistrationDTO dto, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return profileService.createAdminUser(dto, token);
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable String userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        profileService.deleteAdminUser(userId, token);
    }
}
