package com.emall.backend.security;

public record AuthenticatedUser(Long id, String username, Integer role) {
    public boolean isAdmin() {
        return role != null && role >= 1;
    }
}
