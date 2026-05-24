package com.mall.pointsmall.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class CustomerSessionService {
    private static final Duration SESSION_TTL = Duration.ofDays(7);
    private static final String KEY_PREFIX = "customer:active-session:";

    private final StringRedisTemplate redisTemplate;

    public CustomerSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String create(Long customerId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key(customerId), sessionId, SESSION_TTL);
        return sessionId;
    }

    public boolean isActive(Long customerId, String sessionId) {
        return sessionId != null && sessionId.equals(redisTemplate.opsForValue().get(key(customerId)));
    }

    public void logout(Long customerId) {
        redisTemplate.delete(key(customerId));
    }

    private String key(Long customerId) {
        return KEY_PREFIX + customerId;
    }
}
