package com.sophie.aac.auth.util;

import com.sophie.aac.auth.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class SecurityAuthContext implements AuthContext {

  @Override
  public Role currentRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    var authority = auth.getAuthorities().stream().findFirst().orElse(null);
    if (authority == null) return null;
    String roleName = authority.getAuthority().replace("ROLE_", "");
    try {
      return Role.valueOf(roleName);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  @Override
  public UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    Object details = auth.getDetails();
    if (details instanceof Map<?, ?> map) {
      Object userId = map.get("userId");
      if (userId instanceof UUID id) {
        return id;
      }
    }
    return null;
  }

  @Override
  public UUID currentProfileId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    Object details = auth.getDetails();
    if (details instanceof Map<?, ?> map) {
      Object profileId = map.get("profileId");
      if (profileId instanceof UUID id) {
        return id;
      }
    }
    return null;
  }

  @Override
  public UUID currentProfileIdOrDefault() {
    UUID profileId = currentProfileId();
    return profileId != null ? profileId : DEFAULT_PROFILE_ID;
  }

  @Override
  public UUID requireCurrentProfileId() {
    UUID profileId = currentProfileId();
    if (profileId == null) {
      throw new IllegalStateException("No active profile in session. Select a profile after login.");
    }
    return profileId;
  }
}
