package com.emall.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class AuthorizationService {
    public AuthenticatedUser currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    public Long requireSelfOrAdmin(Authentication authentication, Long requestedUserId) {
        AuthenticatedUser current = currentUser(authentication);
        Long effectiveUserId = requestedUserId == null ? current.id() : requestedUserId;
        if (!current.isAdmin() && !Objects.equals(current.id(), effectiveUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能访问其他用户的数据");
        }
        return effectiveUserId;
    }

    public void requireOwnerOrAdmin(Authentication authentication, Long ownerId) {
        AuthenticatedUser current = currentUser(authentication);
        if (!current.isAdmin() && !Objects.equals(current.id(), ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有权限操作该资源");
        }
    }
}
