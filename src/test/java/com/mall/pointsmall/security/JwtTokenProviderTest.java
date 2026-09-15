package com.mall.pointsmall.security;

import com.mall.pointsmall.enums.RoleType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenProviderTest {
    @Test
    void preservesTokenTypeAndSessionForAdminTokens() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "test-signing-secret-that-is-at-least-thirty-two-bytes",
                60, 60, 600
        );
        SecurityUser user = new SecurityUser(7L, "operator", null, RoleType.OPERATOR, Map.of("USERS", "EDIT"), false);

        String accessToken = provider.generateAccessToken(user, "access-session");
        String refreshToken = provider.generateRefreshToken(user, "refresh-session");

        assertEquals("access", provider.tokenType(accessToken));
        assertEquals("access-session", provider.sessionId(accessToken));
        assertEquals(RoleType.OPERATOR, provider.parse(accessToken).getRole());
        assertEquals("refresh", provider.tokenType(refreshToken));
        assertEquals("refresh-session", provider.sessionId(refreshToken));
    }
}
