package com.sophie.aac.auth.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Resolves the active profile (communicator) ID from the current auth context.
 * Set by CookieSessionAuthFilter from auth_session.profile_id.
 */
public final class CurrentProfile {

  private CurrentProfile() {}

  public static UUID get() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    Object details = auth.getDetails();
    if (details instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> map = (java.util.Map<String, Object>) details;
      Object p = map.get("profileId");
      if (p instanceof UUID) return (UUID) p;
    }
    return null;
  }

  /**
   * Returns the profile ID or throws if not set. Use when profile is required (e.g. carer endpoints).
   */
  public static UUID require() {
    UUID id = get();
    if (id == null) {
      throw new IllegalStateException("No active profile in session. Select a profile after login.");
    }
    return id;
  }

  /** Default profile when no session (e.g. unauthenticated dialogue). */
  public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  /**
   * Returns profile ID or default when not authenticated. Use for endpoints that may be called without auth.
   */
  public static UUID getOrDefault() {
    UUID id = get();
    return id != null ? id : DEFAULT_ID;
  }
}
