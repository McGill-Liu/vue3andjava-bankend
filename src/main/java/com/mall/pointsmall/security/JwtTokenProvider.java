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
    private final long refreshTokenSeconds;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.access-token-seconds}") long accessTokenSeconds,
                            @Value("${app.jwt.refresh-token-seconds}") long refreshTokenSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public String generateAccessToken(SecurityUser user) {
        return buildToken(user, accessTokenSeconds, "access");
    }

    public String generateRefreshToken(SecurityUser user) {
        return buildToken(user, refreshTokenSeconds, "refresh");
    }

    public SecurityUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return new SecurityUser(
                Long.valueOf(claims.getSubject()),
                claims.get("name", String.class),
                claims.get("phone", String.class),
                RoleType.valueOf(claims.get("role", String.class)),
                claims.get("permissions", Map.class)
        );
    }

    public String tokenType(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return claims.get("tokenType", String.class);
    }

    private String buildToken(SecurityUser user, long seconds, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("name", user.getName())
                .claim("phone", user.getPhone())
                .claim("role", user.getRole().name())
                .claim("permissions", user.getPermissions())
                .claim("tokenType", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(seconds)))
                .signWith(secretKey)
                .compact();
    }
}
