package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.AuthSessionEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.AuthSessionRepository;
import com.sophie.aac.auth.util.TokenHash;
import com.sophie.aac.common.web.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class AuthSessionService {

  private final AuthSessionRepository sessions;
  private final SecureRandom random = new SecureRandom();

  @Value("${SESSION_TTL_MINUTES:240}")
  private long ttlMinutes;

  public AuthSessionService(AuthSessionRepository sessions) {
    this.sessions = sessions;
  }

  void cleanupExpiredSessions() {
    sessions.deleteByExpiresAtBefore(Instant.now());
  }

  void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    sessions.deleteByTokenHash(TokenHash.sha256Hex(rawToken));
  }

  AuthSessionEntity requireSession(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new ForbiddenException("No session");
    }
    return sessions.findByTokenHash(TokenHash.sha256Hex(rawToken))
        .orElseThrow(() -> new ForbiddenException("Invalid session"));
  }

  List<UUID> getProfileIdsForSession(String rawToken, java.util.function.Function<AuthSessionEntity, List<UUID>> resolver) {
    if (rawToken == null || rawToken.isBlank()) return List.of();
    return sessions.findByTokenHash(TokenHash.sha256Hex(rawToken))
        .map(resolver)
        .orElse(List.of());
  }

  AuthService.LoginResult createSession(Role role, UUID profileId, List<UUID> profileIds, UUID userAccountId, UUID delegatedPinId) {
    cleanupExpiredSessions();
    String token = generateToken();

    AuthSessionEntity session = new AuthSessionEntity();
    session.setId(UUID.randomUUID());
    session.setRole(role);
    session.setTokenHash(TokenHash.sha256Hex(token));
    session.setProfileId(profileId);
    session.setUserAccountId(userAccountId);
    session.setDelegatedPinId(delegatedPinId);
    session.setCreatedAt(Instant.now());
    session.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
    sessions.save(session);

    return new AuthService.LoginResult(role, token, ttlMinutes, profileIds, profileId);
  }

  void save(AuthSessionEntity session) {
    sessions.save(session);
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return UUID.randomUUID() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
