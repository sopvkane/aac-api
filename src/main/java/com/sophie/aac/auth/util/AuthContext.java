package com.sophie.aac.auth.util;

import com.sophie.aac.auth.domain.Role;

import java.util.UUID;

public interface AuthContext {

  UUID DEFAULT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  Role currentRole();

  UUID currentUserId();

  UUID currentProfileId();

  UUID currentProfileIdOrDefault();

  UUID requireCurrentProfileId();
}
