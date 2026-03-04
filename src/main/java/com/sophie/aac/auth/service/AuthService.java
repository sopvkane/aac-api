package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.*;
import com.sophie.aac.auth.repository.*;
import com.sophie.aac.auth.util.PasswordValidator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  public static final String COOKIE_NAME = "AAC_SESSION";

  private final UserAccountRepository userAccounts;
  private final UserAccountProfileRepository userAccountProfiles;
  private final DelegatedPinRepository delegatedPins;
  private final JoiningCodeRepository joiningCodes;
  private final PasswordEncoder encoder;
  private final AuthSessionService sessionService;
  private final AuthProfileAccessService accessService;

  public AuthService(UserAccountRepository userAccounts, UserAccountProfileRepository userAccountProfiles,
      DelegatedPinRepository delegatedPins, JoiningCodeRepository joiningCodes,
      PasswordEncoder encoder,
      AuthSessionService sessionService, AuthProfileAccessService accessService) {
    this.userAccounts = userAccounts;
    this.userAccountProfiles = userAccountProfiles;
    this.delegatedPins = delegatedPins;
    this.joiningCodes = joiningCodes;
    this.encoder = encoder;
    this.sessionService = sessionService;
    this.accessService = accessService;
  }

  @Transactional
  public LoginResult login(Role role, String pin) {
    CaregiverAccountEntity acc = accessService.requireActiveCaregiverAccount(role);

    if (!encoder.matches(pin, acc.getPinHash())) {
      throw new AuthValidationException("Invalid PIN");
    }

    sessionService.cleanupExpiredSessions();
    List<UUID> profileIds = accessService.getProfileIdsForRole(role);

    if (profileIds.isEmpty()) {
      throw new AuthValidationException("No profiles linked to this account");
    }

    return sessionService.createSession(role, profileIds.get(0), profileIds, null, null);
  }

  public List<UUID> getProfileIdsForRole(Role role) {
    return accessService.getProfileIdsForRole(role);
  }

  @Transactional
  public void selectProfile(String rawToken, UUID profileId) {
    AuthSessionEntity session = sessionService.requireSession(rawToken);
    if (!accessService.hasAccessToProfile(session, profileId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "No access to profile");
    }

    session.setProfileId(profileId);
    sessionService.save(session);
  }

  @Transactional
  public void logout(String rawToken) {
    sessionService.logout(rawToken);
  }

  /** Login with email + password (full account) */
  @Transactional
  public LoginResult loginWithPassword(String email, String password) {
    UserAccountEntity user = userAccounts.findByEmailIgnoreCase(email)
        .filter(UserAccountEntity::isActive)
        .orElseThrow(() -> new AuthValidationException("Invalid email or password"));

    if (!encoder.matches(password, user.getPasswordHash())) {
      throw new AuthValidationException("Invalid email or password");
    }

    List<UUID> profileIds = userAccountProfiles.findByUserId(user.getId()).stream()
        .map(UserAccountProfileEntity::getProfileId)
        .toList();
    if (profileIds.isEmpty()) throw new AuthValidationException("No profiles linked");

    Role sessionRole = "CLINICIAN".equals(user.getRole()) ? Role.CLINICIAN : Role.PARENT;
    return sessionService.createSession(sessionRole, profileIds.get(0), profileIds, user.getId(), null);
  }

  /** Login with PIN (delegated access - shared device) */
  @Transactional
  public LoginResult loginWithPin(String pin) {
    if (pin == null || pin.isBlank()) throw new AuthValidationException("Invalid PIN");

    DelegatedPinEntity matched = delegatedPins.findAll().stream()
        .filter(DelegatedPinEntity::isActive)
        .filter(dp -> encoder.matches(pin, dp.getPinHash()))
        .findFirst()
        .orElseThrow(() -> new AuthValidationException("Invalid PIN"));

    List<UUID> profileIds = List.of(matched.getProfileId());
    return sessionService.createSession(Role.CARER, matched.getProfileId(), profileIds, null, matched.getId());
  }

  @Transactional
  public void register(String displayName, String email, String password, String role, String joiningCode) {
    PasswordValidator.validate(password);
    if (!"PARENT_CARER".equals(role) && !"CLINICIAN".equals(role)) {
      throw new AuthValidationException("Role must be PARENT_CARER or CLINICIAN");
    }

    JoiningCodeEntity jc = joiningCodes.findByCodeIgnoreCase(joiningCode)
        .orElseThrow(() -> new AuthValidationException("Invalid joining code"));
    if (!jc.isValid()) throw new AuthValidationException("Joining code expired or used up");

    if (userAccounts.findByEmailIgnoreCase(email).isPresent()) {
      throw new AuthValidationException("Email already registered");
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

  public List<UUID> getProfileIdsForSession(String rawToken) {
    return sessionService.getProfileIdsForSession(rawToken, accessService::getProfileIdsForSession);
  }

  public record LoginResult(Role role, String token, long ttlMinutes, List<UUID> profileIds, UUID activeProfileId) {}
}
