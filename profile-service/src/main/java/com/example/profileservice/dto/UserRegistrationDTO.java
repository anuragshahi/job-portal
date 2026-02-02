package com.example.profileservice.dto;

import com.example.profileservice.model.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegistrationDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid mobile number format (E.164)")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    private Gender gender;

    @Min(value = 0, message = "Age must be a positive number")
    @Max(value = 150, message = "Age must be realistic")
    private Integer age;
}
