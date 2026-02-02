package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Fallback controller for circuit breaker.
 * Returns 503 Service Unavailable when downstream services are unavailable.
 */
@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

        return Mono.just(Map.of(
                "type", "about:blank",
                "title", "Service Unavailable",
                "status", 503,
                "detail", "The downstream service is temporarily unavailable. Please try again later.",
                "path", path
        ));
    }
}
