package com.emall.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceTest {
    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void regularUserCanOnlyAccessOwnResources() {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(7L, "buyer", 0), null, List.of());

        assertThat(authorizationService.requireSelfOrAdmin(authentication, 7L)).isEqualTo(7L);
        assertThatThrownBy(() -> authorizationService.requireSelfOrAdmin(authentication, 8L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void administratorCanAccessAnotherUsersResources() {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(1L, "admin", 2), null, List.of());

        assertThat(authorizationService.requireSelfOrAdmin(authentication, 8L)).isEqualTo(8L);
    }
}
