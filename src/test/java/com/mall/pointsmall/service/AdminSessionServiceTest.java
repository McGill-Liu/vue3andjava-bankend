package com.mall.pointsmall.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionServiceTest {
    @Test
    void createsChecksAndRevokesTheCurrentSession() {
        @SuppressWarnings("unchecked")
        RedisOperations<String, String> redis = mock(RedisOperations.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AdminSessionService service = new AdminSessionService(redis, 600);

        String sessionId = service.create(9L);
        when(values.get("admin:active-session:9")).thenReturn(sessionId);

        assertTrue(service.isActive(9L, sessionId));
        assertFalse(service.isActive(9L, "other-session"));
        verify(values).set(eq("admin:active-session:9"), eq(sessionId), eq(Duration.ofSeconds(600)));

        service.revoke(9L);
        verify(redis).delete("admin:active-session:9");
    }
}
