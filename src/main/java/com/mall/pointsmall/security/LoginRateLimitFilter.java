package com.mall.pointsmall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "rate-limit:auth:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return limitFor(request.getRequestURI()) == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = limitFor(path);
        try {
            String key = KEY_PREFIX + path + ':' + clientIp(request);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW);
            }
            if (count != null && count > limit) {
                tooManyRequests(response);
                return;
            }
        } catch (Exception ex) {
            serviceUnavailable(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private int limitFor(String path) {
        return switch (path) {
            case "/api/auth/admin/login" -> 10;
            case "/api/auth/user/login", "/api/auth/wechat-login" -> 20;
            case "/api/auth/refresh" -> 60;
            default -> 0;
        };
    }

    private String clientIp(HttpServletRequest request) {
        // The backend port must only be reachable through Nginx, which overwrites this header.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void tooManyRequests(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请 1 分钟后再试");
    }

    private void serviceUnavailable(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.SERVICE_UNAVAILABLE, "服务暂不可用，请稍后重试");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
    }
}
