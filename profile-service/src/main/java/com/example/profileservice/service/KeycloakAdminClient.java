package com.example.profileservice.service;

import com.example.profileservice.dto.UserRegistrationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${keycloak.admin-service.url}")
    private String adminServiceUrl;

    public String createUser(UserRegistrationDTO dto, String bearerToken) {
        return restClientBuilder.build().post()
                .uri(adminServiceUrl + "/users")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public void deleteUser(String userId, String bearerToken) {
        restClientBuilder.build().delete()
                .uri(adminServiceUrl + "/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Creates a Keycloak user without password and triggers the UPDATE_PASSWORD action email.
     * Used for self-registration flow where user sets their own password via Keycloak email.
     *
     * @param email     User's email (also used as username)
     * @param username  Username
     * @param firstName First name
     * @param lastName  Last name
     * @return The created Keycloak user ID
     */
    public String createUserWithPasswordAction(String email, String username, String firstName, String lastName) {
        var request = java.util.Map.of(
                "email", email,
                "username", username,
                "firstName", firstName,
                "lastName", lastName,
                "sendPasswordEmail", true
        );

        return restClientBuilder.build().post()
                .uri(adminServiceUrl + "/users/register")
                .body(request)
                .retrieve()
                .body(String.class);
    }
}
