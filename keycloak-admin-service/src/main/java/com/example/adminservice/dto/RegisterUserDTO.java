package com.example.adminservice.dto;

import lombok.Data;

/**
 * DTO for self-registration flow.
 * Creates user without password and optionally triggers password setup email.
 */
@Data
public class RegisterUserDTO {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean sendPasswordEmail;
}
