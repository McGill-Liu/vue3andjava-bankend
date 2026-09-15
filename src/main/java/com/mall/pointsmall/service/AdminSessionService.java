package com.mall.pointsmall.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class AdminSessionService {
    private static final String KEY_PREFIX = "admin:active-session:";

    private final RedisOperations<String, String> redisTemplate;
    private final Duration sessionTtl;

    public AdminSessionService(RedisOperations<String, String> redisTemplate,
                               @Value("${app.jwt.refresh-token-seconds}") long refreshTokenSeconds) {
        this.redisTemplate = redisTemplate;
        this.sessionTtl = Duration.ofSeconds(refreshTokenSeconds);
    }

    public String create(Long adminId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key(adminId), sessionId, sessionTtl);
        return sessionId;
    }

    public boolean isActive(Long adminId, String sessionId) {
        return sessionId != null && sessionId.equals(redisTemplate.opsForValue().get(key(adminId)));
    }

    public void revoke(Long adminId) {
        redisTemplate.delete(key(adminId));
    }

    private String key(Long adminId) {
        return KEY_PREFIX + adminId;
    }
}
