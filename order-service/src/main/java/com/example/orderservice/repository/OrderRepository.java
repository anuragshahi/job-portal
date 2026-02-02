package com.example.orderservice.repository;

import com.example.orderservice.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByCreatedBy(String userId);
}
