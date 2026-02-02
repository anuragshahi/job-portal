package com.example.profileservice.repository;

import com.example.profileservice.model.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileEntityRepository extends JpaRepository<UserProfileEntity, Long> {
    Optional<UserProfileEntity> findByUserId(String userId);
    Optional<UserProfileEntity> findByEmail(String email);
    void deleteByUserId(String userId);
}