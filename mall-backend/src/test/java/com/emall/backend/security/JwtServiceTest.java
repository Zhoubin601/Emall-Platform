package com.emall.backend.security;

import com.emall.backend.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void issuesAndParsesSignedToken() {
        JwtService jwtService = new JwtService(
                "test-only-jwt-secret-with-at-least-32-characters",
                60_000);
        User user = new User();
        user.setId(42L);
        user.setUsername("tester");

        String token = jwtService.issueToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }
}
