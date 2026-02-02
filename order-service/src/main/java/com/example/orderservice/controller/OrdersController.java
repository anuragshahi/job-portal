package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for order operations.
 * <p>
 * External path: /orders/ (via Gateway)
 * Internal path: /api/ (after Gateway transforms /orders/ -> /api/)
 * <p>
 * Trailing slash normalization is handled by {@code TrailingSlashFilter} in common-web.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest orderRequest, @AuthenticationPrincipal Jwt jwt) {
        return orderService.createOrder(orderRequest, jwt.getSubject());
    }

    @GetMapping
    public List<OrderResponse> getOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getOrdersByUserId(jwt.getSubject());
    }
}
