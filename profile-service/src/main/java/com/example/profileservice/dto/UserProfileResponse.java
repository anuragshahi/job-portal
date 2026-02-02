package com.example.profileservice.dto;

import com.example.profileservice.model.UserProfileEntity;
import com.example.profileservice.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private Gender gender;
    private Integer age;
    private Boolean enabled;

    public static UserProfileResponse fromEntity(UserProfileEntity entity) {
        return UserProfileResponse.builder()
                .userId(entity.getUserId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .mobileNumber(entity.getMobileNumber())
                .gender(entity.getGender())
                .age(entity.getAge())
                .enabled(entity.getEnabled())
                .build();
    }
}