package com.mall.pointsmall.security;

import com.mall.pointsmall.enums.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenSeconds;
    private final long customerAccessTokenSeconds;
    private final long refreshTokenSeconds;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.access-token-seconds}") long accessTokenSeconds,
                            @Value("${app.jwt.customer-access-token-seconds}") long customerAccessTokenSeconds,
                            @Value("${app.jwt.refresh-token-seconds}") long refreshTokenSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds = accessTokenSeconds;
        this.customerAccessTokenSeconds = customerAccessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public String generateAccessToken(SecurityUser user, String sessionId) {
        return buildToken(user, accessTokenSeconds, "access", sessionId);
    }

    public String generateCustomerAccessToken(SecurityUser user, String sessionId) {
        return buildToken(user, customerAccessTokenSeconds, "access", sessionId);
    }

    public String generateRefreshToken(SecurityUser user, String sessionId) {
        return buildToken(user, refreshTokenSeconds, "refresh", sessionId);
    }

    public SecurityUser parse(String token) {
        Claims claims = claims(token);
        return new SecurityUser(
                Long.valueOf(claims.getSubject()),
                claims.get("name", String.class),
                claims.get("phone", String.class),
                RoleType.valueOf(claims.get("role", String.class)),
                claims.get("permissions", Map.class),
                Boolean.TRUE.equals(claims.get("passwordChangeRequired", Boolean.class))
        );
    }

    public String sessionId(String token) {
        return claims(token).get("sessionId", String.class);
    }

    public String tokenType(String token) {
        return claims(token).get("tokenType", String.class);
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    private String buildToken(SecurityUser user, long seconds, String type, String sessionId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("name", user.getName())
                .claim("phone", user.getPhone())
                .claim("role", user.getRole().name())
                .claim("permissions", user.getPermissions())
                .claim("passwordChangeRequired", user.isPasswordChangeRequired())
                .claim("tokenType", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(seconds)));
        if (sessionId != null) {
            builder.claim("sessionId", sessionId);
        }
        return builder.signWith(secretKey).compact();
    }
}
