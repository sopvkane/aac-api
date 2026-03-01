package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.AuthSessionEntity;
import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.AuthSessionRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.util.TokenHash;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  public static final String COOKIE_NAME = "AAC_SESSION";

  private final CaregiverAccountRepository accounts;
  private final AuthSessionRepository sessions;
  private final PasswordEncoder encoder;

  private final SecureRandom random = new SecureRandom();

  @Value("${SESSION_TTL_MINUTES:240}")
  private long ttlMinutes;

  public AuthService(CaregiverAccountRepository accounts, AuthSessionRepository sessions, PasswordEncoder encoder) {
    this.accounts = accounts;
    this.sessions = sessions;
    this.encoder = encoder;
  }

  public LoginResult login(Role role, String pin) {
    CaregiverAccountEntity acc = accounts.findByRole(role)
        .filter(CaregiverAccountEntity::isActive)
        .orElseThrow(() -> new IllegalArgumentException("Invalid role or account inactive"));

    if (!encoder.matches(pin, acc.getPinHash())) {
      throw new IllegalArgumentException("Invalid PIN");
    }

    // Cleanup expired sessions occasionally
    sessions.deleteByExpiresAtBefore(Instant.now());

    String token = generateToken();
    String tokenHash = TokenHash.sha256Hex(token);

    AuthSessionEntity s = new AuthSessionEntity();
    s.setId(UUID.randomUUID());
    s.setRole(role);
    s.setTokenHash(tokenHash);
    s.setCreatedAt(Instant.now());
    s.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
    sessions.save(s);

    return new LoginResult(role, token, ttlMinutes);
  }

  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    sessions.deleteByTokenHash(TokenHash.sha256Hex(rawToken));
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return UUID.randomUUID() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record LoginResult(Role role, String token, long ttlMinutes) {}
}