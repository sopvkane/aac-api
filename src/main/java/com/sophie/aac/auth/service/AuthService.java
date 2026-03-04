package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.*;
import com.sophie.aac.auth.repository.*;
import com.sophie.aac.auth.util.PasswordValidator;
import com.sophie.aac.auth.util.TokenHash;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  public static final String COOKIE_NAME = "AAC_SESSION";

  private final CaregiverAccountRepository accounts;
  private final CaregiverAccountProfileRepository accountProfiles;
  private final UserAccountRepository userAccounts;
  private final UserAccountProfileRepository userAccountProfiles;
  private final DelegatedPinRepository delegatedPins;
  private final JoiningCodeRepository joiningCodes;
  private final AuthSessionRepository sessions;
  private final PasswordEncoder encoder;

  private final SecureRandom random = new SecureRandom();

  @Value("${SESSION_TTL_MINUTES:240}")
  private long ttlMinutes;

  public AuthService(CaregiverAccountRepository accounts, CaregiverAccountProfileRepository accountProfiles,
      UserAccountRepository userAccounts, UserAccountProfileRepository userAccountProfiles,
      DelegatedPinRepository delegatedPins, JoiningCodeRepository joiningCodes,
      AuthSessionRepository sessions, PasswordEncoder encoder) {
    this.accounts = accounts;
    this.accountProfiles = accountProfiles;
    this.userAccounts = userAccounts;
    this.userAccountProfiles = userAccountProfiles;
    this.delegatedPins = delegatedPins;
    this.joiningCodes = joiningCodes;
    this.sessions = sessions;
    this.encoder = encoder;
  }

  @Transactional
  public LoginResult login(Role role, String pin) {
    CaregiverAccountEntity acc = accounts.findByRole(role)
        .filter(CaregiverAccountEntity::isActive)
        .orElseThrow(() -> new IllegalArgumentException("Invalid role or account inactive"));

    if (!encoder.matches(pin, acc.getPinHash())) {
      throw new IllegalArgumentException("Invalid PIN");
    }

    // Cleanup expired sessions occasionally
    sessions.deleteByExpiresAtBefore(Instant.now());

    List<UUID> profileIds = accountProfiles.findByAccountId(acc.getId()).stream()
        .map(CaregiverAccountProfileEntity::getProfileId)
        .toList();

    if (profileIds.isEmpty()) {
      throw new IllegalArgumentException("No profiles linked to this account");
    }

    String token = generateToken();
    String tokenHash = TokenHash.sha256Hex(token);

    UUID defaultProfileId = profileIds.get(0);
    AuthSessionEntity s = new AuthSessionEntity();
    s.setId(UUID.randomUUID());
    s.setRole(role);
    s.setTokenHash(tokenHash);
    s.setProfileId(defaultProfileId);
    s.setCreatedAt(Instant.now());
    s.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
    sessions.save(s);

    return new LoginResult(role, token, ttlMinutes, profileIds, defaultProfileId);
  }

  public List<UUID> getProfileIdsForRole(Role role) {
    return accounts.findByRole(role)
        .map(acc -> accountProfiles.findByAccountId(acc.getId()).stream()
            .map(CaregiverAccountProfileEntity::getProfileId)
            .toList())
        .orElse(List.of());
  }

  @Transactional
  public void selectProfile(String rawToken, UUID profileId) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "No session");
    }
    String hash = TokenHash.sha256Hex(rawToken);
    AuthSessionEntity session = sessions.findByTokenHash(hash)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN, "Invalid session"));

    boolean hasAccess;
    if (session.getUserAccountId() != null) {
      hasAccess = userAccountProfiles.findByUserId(session.getUserAccountId()).stream()
          .anyMatch(up -> up.getProfileId().equals(profileId));
    } else if (session.getDelegatedPinId() != null) {
      hasAccess = delegatedPins.findById(session.getDelegatedPinId())
          .filter(DelegatedPinEntity::isActive)
          .map(dp -> dp.getProfileId().equals(profileId))
          .orElse(false);
    } else {
      CaregiverAccountEntity acc = accounts.findByRole(session.getRole())
          .filter(CaregiverAccountEntity::isActive)
          .orElse(null);
      hasAccess = acc != null && accountProfiles.findByAccountId(acc.getId()).stream()
          .anyMatch(ap -> ap.getProfileId().equals(profileId));
    }
    if (!hasAccess) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "No access to profile");
    }

    session.setProfileId(profileId);
    sessions.save(session);
  }

  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    sessions.deleteByTokenHash(TokenHash.sha256Hex(rawToken));
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return UUID.randomUUID() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Login with email + password (full account) */
  @Transactional
  public LoginResult loginWithPassword(String email, String password) {
    UserAccountEntity user = userAccounts.findByEmailIgnoreCase(email)
        .filter(UserAccountEntity::isActive)
        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

    if (!encoder.matches(password, user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    List<UUID> profileIds = userAccountProfiles.findByUserId(user.getId()).stream()
        .map(UserAccountProfileEntity::getProfileId)
        .toList();
    if (profileIds.isEmpty()) throw new IllegalArgumentException("No profiles linked");

    Role sessionRole = "CLINICIAN".equals(user.getRole()) ? Role.CLINICIAN : Role.PARENT;
    return createSession(sessionRole, profileIds.get(0), profileIds, user.getId(), null);
  }

  /** Login with PIN (delegated access - shared device) */
  @Transactional
  public LoginResult loginWithPin(String pin) {
    if (pin == null || pin.isBlank()) throw new IllegalArgumentException("Invalid PIN");

    DelegatedPinEntity matched = delegatedPins.findAll().stream()
        .filter(DelegatedPinEntity::isActive)
        .filter(dp -> encoder.matches(pin, dp.getPinHash()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid PIN"));

    List<UUID> profileIds = List.of(matched.getProfileId());
    return createSession(Role.CARER, matched.getProfileId(), profileIds, null, matched.getId());
  }

  @Transactional
  public void register(String displayName, String email, String password, String role, String joiningCode) {
    PasswordValidator.validate(password);
    if (!"PARENT_CARER".equals(role) && !"CLINICIAN".equals(role)) {
      throw new IllegalArgumentException("Role must be PARENT_CARER or CLINICIAN");
    }

    JoiningCodeEntity jc = joiningCodes.findByCodeIgnoreCase(joiningCode)
        .orElseThrow(() -> new IllegalArgumentException("Invalid joining code"));
    if (!jc.isValid()) throw new IllegalArgumentException("Joining code expired or used up");

    if (userAccounts.findByEmailIgnoreCase(email).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    UserAccountEntity user = new UserAccountEntity();
    user.setId(UUID.randomUUID());
    user.setEmail(email.trim().toLowerCase());
    user.setPasswordHash(encoder.encode(password));
    user.setDisplayName(displayName != null ? displayName.trim() : "User");
    user.setRole(role);
    user.setActive(true);
    user.setCreatedAt(Instant.now());
    userAccounts.save(user);

    var link = new UserAccountProfileEntity();
    link.setUserId(user.getId());
    link.setProfileId(com.sophie.aac.profile.service.CaregiverProfileService.DEFAULT_ID);
    userAccountProfiles.save(link);

    jc.setUsedCount(jc.getUsedCount() + 1);
    joiningCodes.save(jc);
  }

  private LoginResult createSession(Role role, UUID profileId, List<UUID> profileIds, UUID userAccountId, UUID delegatedPinId) {
    sessions.deleteByExpiresAtBefore(Instant.now());
    String token = generateToken();
    AuthSessionEntity s = new AuthSessionEntity();
    s.setId(UUID.randomUUID());
    s.setRole(role);
    s.setTokenHash(TokenHash.sha256Hex(token));
    s.setProfileId(profileId);
    s.setUserAccountId(userAccountId);
    s.setDelegatedPinId(delegatedPinId);
    s.setCreatedAt(Instant.now());
    s.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
    sessions.save(s);
    return new LoginResult(role, token, ttlMinutes, profileIds, profileId);
  }

  public List<UUID> getProfileIdsForSession(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return List.of();
    return sessions.findByTokenHash(TokenHash.sha256Hex(rawToken))
        .map(session -> {
          if (session.getUserAccountId() != null) {
            return userAccountProfiles.findByUserId(session.getUserAccountId()).stream()
                .map(UserAccountProfileEntity::getProfileId)
                .toList();
          }
          if (session.getDelegatedPinId() != null) {
            return delegatedPins.findById(session.getDelegatedPinId())
                .filter(DelegatedPinEntity::isActive)
                .map(dp -> List.of(dp.getProfileId()))
                .orElse(List.of());
          }
          return getProfileIdsForRole(session.getRole());
        })
        .orElse(List.of());
  }

  public record LoginResult(Role role, String token, long ttlMinutes, List<UUID> profileIds, UUID activeProfileId) {}
}