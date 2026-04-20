package com.sophie.aac.auth.util;

import com.sophie.aac.auth.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAuthContextTest {

  private final SecurityAuthContext ctx = new SecurityAuthContext();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returns_nulls_when_no_authentication() {
    assertThat(ctx.currentRole()).isNull();
    assertThat(ctx.currentUserId()).isNull();
    assertThat(ctx.currentProfileId()).isNull();
    assertThat(ctx.currentProfileIdOrDefault()).isEqualTo(AuthContext.DEFAULT_PROFILE_ID);
    assertThatThrownBy(ctx::requireCurrentProfileId)
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void extracts_role_user_and_profile_ids_from_details() {
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();
    var auth = new UsernamePasswordAuthenticationToken(
        "principal",
        null,
        AuthorityUtils.createAuthorityList("ROLE_PARENT")
    );
    auth.setDetails(Map.of("userId", userId, "profileId", profileId));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(ctx.currentRole()).isEqualTo(Role.PARENT);
    assertThat(ctx.currentUserId()).isEqualTo(userId);
    assertThat(ctx.currentProfileId()).isEqualTo(profileId);
    assertThat(ctx.currentProfileIdOrDefault()).isEqualTo(profileId);
    assertThat(ctx.requireCurrentProfileId()).isEqualTo(profileId);
  }

  @Test
  void handles_invalid_role_and_non_uuid_details_gracefully() {
    var auth = new UsernamePasswordAuthenticationToken(
        "principal",
        null,
        AuthorityUtils.createAuthorityList("ROLE_NOT_A_REAL_ROLE")
    );
    auth.setDetails(Map.of("userId", "not-uuid", "profileId", "not-uuid"));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(ctx.currentRole()).isNull();
    assertThat(ctx.currentUserId()).isNull();
    assertThat(ctx.currentProfileId()).isNull();
    assertThat(ctx.currentProfileIdOrDefault()).isEqualTo(AuthContext.DEFAULT_PROFILE_ID);
  }
}

