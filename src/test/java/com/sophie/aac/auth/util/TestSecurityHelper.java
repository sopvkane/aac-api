package com.sophie.aac.auth.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

/**
 * Helper for tests that need SecurityContext with role and profile.
 */
public final class TestSecurityHelper {

  public static final UUID DEFAULT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private TestSecurityHelper() {}

  /** Sets SecurityContext with PARENT role and default profile. */
  public static void setParentWithProfile() {
    setRoleWithProfile("PARENT");
  }

  /** Sets SecurityContext with given role and default profile. */
  public static void setRoleWithProfile(String role) {
    var auth = new UsernamePasswordAuthenticationToken(
        "test",
        null,
        AuthorityUtils.createAuthorityList("ROLE_" + role)
    );
    auth.setDetails(Map.of("profileId", DEFAULT_PROFILE_ID));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  /** Sets SecurityContext with user account ID (for CurrentUser) and profile. */
  public static void setUserWithProfile(UUID userId) {
    var auth = new UsernamePasswordAuthenticationToken(
        "test",
        null,
        AuthorityUtils.createAuthorityList("ROLE_PARENT")
    );
    auth.setDetails(Map.of("userId", userId, "profileId", DEFAULT_PROFILE_ID));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  public static void clear() {
    SecurityContextHolder.clearContext();
  }
}
