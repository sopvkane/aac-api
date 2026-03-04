package com.sophie.aac.auth.util;

import com.sophie.aac.auth.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resolve current user's role from SecurityContext. */
public final class CurrentRole {

    private CurrentRole() {}

    public static Role get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var authority = auth.getAuthorities().stream().findFirst().orElse(null);
        if (authority == null) return null;
        String roleName = authority.getAuthority().replace("ROLE_", "");
        try {
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
