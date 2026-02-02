package com.example.profileservice.service;

import com.example.profileservice.dto.UserRegistrationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${keycloak.admin-service.url}")
    private String adminServiceUrl;

    public String createUser(UserRegistrationDTO dto, String bearerToken) {
        return webClientBuilder.build().post()
                .uri(adminServiceUrl + "/users")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public void deleteUser(String userId, String bearerToken) {
        webClientBuilder.build().delete()
                .uri(adminServiceUrl + "/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .toBodilessEntity()
                .block();
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

        return webClientBuilder.build().post()
                .uri(adminServiceUrl + "/users/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
