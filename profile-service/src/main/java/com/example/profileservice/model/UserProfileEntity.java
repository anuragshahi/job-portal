package com.example.profileservice.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userId; // The ID from Keycloak/OIDC

    private String firstName;
    private String lastName;
    
    @Column(unique = true)
    private String email;

    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer age;

    /**
     * Indicates whether the user has completed registration and can login.
     * Set to false during registration, true after email/mobile confirmation.
     *
     * <p><b>Why Boolean wrapper instead of primitive boolean?</b></p>
     * Jackson uses the no-args constructor (from @NoArgsConstructor) for deserialization,
     * which does NOT apply @Builder.Default values. With primitive {@code boolean}, when
     * the JSON payload omits this field, Jackson fails with:
     * "Cannot map null into type boolean".
     *
     * <p><b>Why @JsonSetter(nulls = Nulls.SKIP)?</b></p>
     * This tells Jackson to skip setting this field when the JSON value is explicitly null
     * (e.g., {"enabled": null}), preserving the field's initialized default value (false).
     * Without this, an explicit null would overwrite the default.
     *
     * <p><b>Service layer fallback:</b></p>
     * ProfileService.createProfile() also checks for null and sets a default, providing
     * defense-in-depth for cases where deserialization might still result in null.
     *
     * @see com.example.profileservice.service.ProfileService#createProfile
     */
    @Column(nullable = false)
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean enabled = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfileEntity that = (UserProfileEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}