package com.example.bff.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SessionRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${bff.session.ttl-minutes}")
    private int sessionTtlMinutes;

    public void save(String jti, OAuth2AuthorizedClient client) {
        // In a real app, you would encrypt the token here
        redisTemplate.opsForValue().set(jti, client, Duration.ofMinutes(sessionTtlMinutes));
    }

    public void saveIdToken(String jti, String idToken) {
        redisTemplate.opsForValue().set(jti + ":id_token", idToken, Duration.ofMinutes(sessionTtlMinutes));
    }

    public OAuth2AuthorizedClient load(String jti) {
        // In a real app, you would decrypt here
        return (OAuth2AuthorizedClient) redisTemplate.opsForValue().get(jti);
    }

    public String loadIdToken(String jti) {
        return (String) redisTemplate.opsForValue().get(jti + ":id_token");
    }

    public void delete(String jti) {
        redisTemplate.delete(jti);
        redisTemplate.delete(jti + ":id_token");
    }
}
