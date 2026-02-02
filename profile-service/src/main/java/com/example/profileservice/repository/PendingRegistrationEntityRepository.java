package com.example.profileservice.repository;

import com.example.profileservice.model.PendingRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingRegistrationEntityRepository extends JpaRepository<PendingRegistrationEntity, Long> {
    Optional<PendingRegistrationEntity> findByConfirmationToken(String confirmationToken);
}