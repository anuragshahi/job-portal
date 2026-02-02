package com.example.profileservice.controller;

import com.example.profileservice.dto.UserProfileRequest;
import com.example.profileservice.dto.UserProfileResponse;
import com.example.profileservice.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user profile operations.
 * <p>
 * External path: /profile/ (via Gateway)
 * Internal path: /api/ (after Gateway transforms /profile/ -> /api/)
 * <p>
 * Trailing slash normalization is handled by {@code TrailingSlashFilter} in common-web.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.getProfile(jwt.getSubject()));
    }

    @PostMapping
    public UserProfileResponse createProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserProfileRequest request) {
        return profileService.createProfile(request, jwt.getSubject());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@AuthenticationPrincipal Jwt jwt) {
        profileService.deleteProfile(jwt.getSubject());
    }
}
