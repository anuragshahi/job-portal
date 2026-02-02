package com.example.orderservice.dto;

import com.example.orderservice.model.OrderEntity;
import com.example.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderNumber;
    private OrderStatus status;
    private String createdBy;
    private LocalDateTime creationTime;

    public static OrderResponse fromEntity(OrderEntity entity) {
        return OrderResponse.builder()
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .creationTime(entity.getCreationTime())
                .build();
    }
}
