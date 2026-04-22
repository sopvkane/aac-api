package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.CaregiverAccountProfileEntity;
import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.domain.JoiningCodeEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.domain.UserAccountEntity;
import com.sophie.aac.auth.domain.UserAccountProfileEntity;
import com.sophie.aac.auth.repository.AuthSessionRepository;
import com.sophie.aac.auth.repository.CaregiverAccountProfileRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.repository.DelegatedPinRepository;
import com.sophie.aac.auth.repository.JoiningCodeRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.repository.UserAccountRepository;
import com.sophie.aac.auth.util.AuthContext;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "PARENT_PIN=1234",
    "CARER_PIN=5678",
    "CLINICIAN_PIN=9012"
})
class AuthServiceTest {

  @Autowired
  AuthService authService;

  @Autowired
  CaregiverAccountRepository accountRepo;

  @Autowired
  AuthSessionRepository sessionRepo;

  @Autowired
  CaregiverAccountProfileRepository accountProfileRepo;

  @Autowired
  com.sophie.aac.profile.repository.UserProfileRepository profileRepo;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  UserAccountRepository userAccountRepo;

  @Autowired
  UserAccountProfileRepository userAccountProfileRepo;

  @Autowired
  JoiningCodeRepository joiningCodeRepo;

  @Autowired
  DelegatedPinRepository delegatedPinRepo;

  @BeforeEach
  void setUp() {
    sessionRepo.deleteAll();
    delegatedPinRepo.deleteAll();
    userAccountProfileRepo.deleteAll();
    joiningCodeRepo.deleteAll();
    userAccountRepo.deleteAll();
    accountProfileRepo.deleteAll();
    accountRepo.deleteAll();
    ensureDefaultProfile();
    seedParentAccount();
  }

  private void ensureDefaultProfile() {
    if (profileRepo.findById(AuthContext.DEFAULT_PROFILE_ID).isEmpty()) {
      var p = new com.sophie.aac.profile.domain.UserProfileEntity();
      p.setId(AuthContext.DEFAULT_PROFILE_ID);
      p.setDisplayName("Test");
      p.setWakeName("Hey");
      p.setDetailsDefault(true);
      p.setVoiceDefault(false);
      p.setAiEnabled(true);
      p.setMemoryEnabled(true);
      p.setAnalyticsEnabled(false);
      p.setDefaultLocation(com.sophie.aac.suggestions.domain.LocationCategory.HOME);
      p.setAllowHome(true);
      p.setAllowSchool(true);
      p.setAllowWork(false);
      p.setAllowOther(true);
      p.setMaxOptions(3);
      p.setPreferredIconSize("large");
      p.setUpdatedAt(java.time.Instant.now());
      profileRepo.save(p);
    }
  }

  private void seedParentAccount() {
    CaregiverAccountEntity acc = new CaregiverAccountEntity();
    acc.setId(java.util.UUID.randomUUID());
    acc.setRole(Role.PARENT);
    acc.setPinHash(encoder.encode("1234"));
    acc.setActive(true);
    acc.setCreatedAt(java.time.Instant.now());
    accountRepo.save(acc);

    CaregiverAccountProfileEntity ap = new CaregiverAccountProfileEntity();
    ap.setAccountId(acc.getId());
    ap.setProfileId(AuthContext.DEFAULT_PROFILE_ID);
    accountProfileRepo.save(ap);
  }

  @Test
  void login_success_returns_token() {
    AuthService.LoginResult result = authService.login(Role.PARENT, "1234");
    assertThat(result.role()).isEqualTo(Role.PARENT);
    assertThat(result.token()).isNotBlank();
    assertThat(result.ttlMinutes()).isPositive();
    assertThat(result.profileIds()).contains(AuthContext.DEFAULT_PROFILE_ID);
    assertThat(result.activeProfileId()).isEqualTo(AuthContext.DEFAULT_PROFILE_ID);

    assertThat(sessionRepo.count()).isEqualTo(1);
  }

  @Test
  void login_invalid_pin_throws() {
    assertThatThrownBy(() -> authService.login(Role.PARENT, "9999"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid PIN");
  }

  @Test
  void login_missing_account_throws() {
    assertThatThrownBy(() -> authService.login(Role.CLINICIAN, "9012"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid role");
  }

  @Test
  void logout_removes_session() {
    AuthService.LoginResult login = authService.login(Role.PARENT, "1234");
    assertThat(sessionRepo.count()).isEqualTo(1);

    authService.logout(login.token());
    assertThat(sessionRepo.count()).isEqualTo(0);
  }

  @Test
  void logout_null_token_is_noop() {
    authService.login(Role.PARENT, "1234");
    authService.logout(null);
    assertThat(sessionRepo.count()).isEqualTo(1);
  }

  @Test
  void logout_blank_token_is_noop() {
    authService.login(Role.PARENT, "1234");
    authService.logout("   ");
    assertThat(sessionRepo.count()).isEqualTo(1);
  }

  @Test
  void selectProfile_success_updates_session() {
    AuthService.LoginResult login = authService.login(Role.PARENT, "1234");
    var sessionBefore = sessionRepo.findAll().get(0);
    assertThat(sessionBefore.getProfileId()).isEqualTo(AuthContext.DEFAULT_PROFILE_ID);

    authService.selectProfile(login.token(), AuthContext.DEFAULT_PROFILE_ID);
    var sessionAfter = sessionRepo.findAll().get(0);
    assertThat(sessionAfter.getProfileId()).isEqualTo(AuthContext.DEFAULT_PROFILE_ID);
  }

  @Test
  void selectProfile_forbidden_when_no_access_to_profile() {
    var otherProfileId = java.util.UUID.randomUUID();
    var p = new com.sophie.aac.profile.domain.UserProfileEntity();
    p.setId(otherProfileId);
    p.setDisplayName("Other");
    p.setWakeName("Hey");
    p.setDetailsDefault(true);
    p.setVoiceDefault(false);
    p.setAiEnabled(true);
    p.setMemoryEnabled(true);
    p.setAnalyticsEnabled(false);
    p.setDefaultLocation(com.sophie.aac.suggestions.domain.LocationCategory.HOME);
    p.setAllowHome(true);
    p.setAllowSchool(true);
    p.setAllowWork(false);
    p.setAllowOther(true);
    p.setMaxOptions(3);
    p.setPreferredIconSize("large");
    p.setUpdatedAt(java.time.Instant.now());
    profileRepo.save(p);

    AuthService.LoginResult login = authService.login(Role.PARENT, "1234");
    assertThatThrownBy(() -> authService.selectProfile(login.token(), otherProfileId))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("No access to profile");
  }

  // --- New auth: email+password, PIN, register ---

  private void seedUserAccount(String email, String password, String role) {
    UserAccountEntity u = new UserAccountEntity();
    u.setId(UUID.randomUUID());
    u.setEmail(email);
    u.setPasswordHash(encoder.encode(password));
    u.setDisplayName("Test User");
    u.setRole(role);
    u.setActive(true);
    u.setCreatedAt(Instant.now());
    userAccountRepo.save(u);

    UserAccountProfileEntity link = new UserAccountProfileEntity();
    link.setUserId(u.getId());
    link.setProfileId(AuthContext.DEFAULT_PROFILE_ID);
    userAccountProfileRepo.save(link);
  }

  private void seedJoiningCode(String code, UUID createdByUserId) {
    JoiningCodeEntity jc = new JoiningCodeEntity();
    jc.setId(UUID.randomUUID());
    jc.setCode(code);
    jc.setCreatedByUserId(createdByUserId);
    jc.setExpiresAt(Instant.now().plus(Duration.ofDays(365)));
    jc.setMaxUses(100);
    jc.setUsedCount(0);
    jc.setCreatedAt(Instant.now());
    joiningCodeRepo.save(jc);
  }

  private void seedDelegatedPin(String pin, UUID profileId) {
    DelegatedPinEntity dp = new DelegatedPinEntity();
    dp.setId(UUID.randomUUID());
    dp.setPinHash(encoder.encode(pin));
    dp.setLabel("Test");
    dp.setCreatedByUserId(UUID.randomUUID());
    dp.setProfileId(profileId);
    dp.setActive(true);
    dp.setCreatedAt(Instant.now());
    delegatedPinRepo.save(dp);
  }

  @Test
  void loginWithPassword_success_returns_token() {
    seedUserAccount("user@test.com", "Password1!", "PARENT_CARER");

    AuthService.LoginResult result = authService.loginWithPassword("user@test.com", "Password1!");

    assertThat(result.role()).isEqualTo(Role.PARENT);
    assertThat(result.token()).isNotBlank();
    assertThat(result.profileIds()).contains(AuthContext.DEFAULT_PROFILE_ID);
    assertThat(sessionRepo.count()).isEqualTo(1);
  }

  @Test
  void loginWithPassword_invalid_email_throws() {
    seedUserAccount("user@test.com", "Password1!", "PARENT_CARER");

    assertThatThrownBy(() -> authService.loginWithPassword("wrong@test.com", "Password1!"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email or password");
  }

  @Test
  void loginWithPassword_invalid_password_throws() {
    seedUserAccount("user@test.com", "Password1!", "PARENT_CARER");

    assertThatThrownBy(() -> authService.loginWithPassword("user@test.com", "WrongPass1!"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email or password");
  }

  @Test
  void loginWithPin_success_returns_token() {
    seedDelegatedPin("5678", AuthContext.DEFAULT_PROFILE_ID);

    AuthService.LoginResult result = authService.loginWithPin("5678");

    assertThat(result.role()).isEqualTo(Role.CARER);
    assertThat(result.profileIds()).containsExactly(AuthContext.DEFAULT_PROFILE_ID);
    assertThat(sessionRepo.count()).isEqualTo(1);
  }

  @Test
  void loginWithPin_falls_back_to_role_pin_when_no_delegated_pin_matches() {
    AuthService.LoginResult result = authService.loginWithPin("1234");

    assertThat(result.role()).isEqualTo(Role.PARENT);
    assertThat(result.profileIds()).containsExactly(AuthContext.DEFAULT_PROFILE_ID);
    assertThat(sessionRepo.count()).isEqualTo(1);
  }

  @Test
  void loginWithPin_invalid_pin_throws() {
    seedDelegatedPin("5678", AuthContext.DEFAULT_PROFILE_ID);

    assertThatThrownBy(() -> authService.loginWithPin("9999"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid PIN");
  }

  @Test
  void loginWithPin_blank_pin_throws() {
    assertThatThrownBy(() -> authService.loginWithPin("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid PIN");
  }

  @Test
  void register_success_creates_user_and_links_profile() {
    seedUserAccount("admin@test.com", "Admin123!", "CLINICIAN");
    seedJoiningCode("JOIN2024", userAccountRepo.findByEmailIgnoreCase("admin@test.com").orElseThrow().getId());

    authService.register("New User", "new@test.com", "Password1!@#", "PARENT_CARER", "JOIN2024");

    assertThat(userAccountRepo.findByEmailIgnoreCase("new@test.com")).isPresent();
    assertThat(userAccountProfileRepo.findByUserId(
        userAccountRepo.findByEmailIgnoreCase("new@test.com").orElseThrow().getId()))
        .hasSize(1)
        .element(0).matches(up -> up.getProfileId().equals(AuthContext.DEFAULT_PROFILE_ID));
  }

  @Test
  void register_invalid_joining_code_throws() {
    assertThatThrownBy(() -> authService.register("User", "u@t.com", "Password1!@#", "PARENT_CARER", "INVALID"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid joining code");
  }

  @Test
  void register_email_already_registered_throws() {
    seedUserAccount("exist@test.com", "Pass123!@#", "PARENT_CARER");
    UUID clinicianId = userAccountRepo.findByEmailIgnoreCase("exist@test.com").orElseThrow().getId();
    seedJoiningCode("CODE1", clinicianId);

    assertThatThrownBy(() -> authService.register("Other", "exist@test.com", "Password1!@#", "PARENT_CARER", "CODE1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email already registered");
  }

  @Test
  void register_invalid_role_throws() {
    seedUserAccount("admin@test.com", "Admin123!", "CLINICIAN");
    seedJoiningCode("CODE2", userAccountRepo.findByEmailIgnoreCase("admin@test.com").orElseThrow().getId());

    assertThatThrownBy(() -> authService.register("User", "u@t.com", "Password1!@#", "INVALID_ROLE", "CODE2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role must be PARENT_CARER or CLINICIAN");
  }

  @Test
  void getProfileIdsForSession_with_user_account_returns_profiles() {
    seedUserAccount("user@test.com", "Password1!", "PARENT_CARER");
    AuthService.LoginResult login = authService.loginWithPassword("user@test.com", "Password1!");

    var ids = authService.getProfileIdsForSession(login.token());
    assertThat(ids).containsExactly(AuthContext.DEFAULT_PROFILE_ID);
  }

  @Test
  void getProfileIdsForSession_with_delegated_pin_returns_single_profile() {
    seedDelegatedPin("5678", AuthContext.DEFAULT_PROFILE_ID);
    AuthService.LoginResult login = authService.loginWithPin("5678");

    var ids = authService.getProfileIdsForSession(login.token());
    assertThat(ids).containsExactly(AuthContext.DEFAULT_PROFILE_ID);
  }

  @Test
  void getProfileIdsForSession_with_role_login_returns_linked_profiles() {
    AuthService.LoginResult login = authService.login(Role.PARENT, "1234");

    var ids = authService.getProfileIdsForSession(login.token());
    assertThat(ids).containsExactly(AuthContext.DEFAULT_PROFILE_ID);
  }
}
