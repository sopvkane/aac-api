package com.sophie.aac.auth.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Resolves the current user_account ID from the auth context (full account sessions only). */
public final class CurrentUser {

  private CurrentUser() {}

  public static UUID get() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    Object details = auth.getDetails();
    if (details instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> map = (java.util.Map<String, Object>) details;
      Object u = map.get("userId");
      if (u instanceof UUID) return (UUID) u;
    }
    return null;
  }
}
