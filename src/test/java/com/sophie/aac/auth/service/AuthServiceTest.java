package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.CaregiverAccountProfileEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.AuthSessionRepository;
import com.sophie.aac.auth.repository.CaregiverAccountProfileRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.util.CurrentProfile;
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

  @BeforeEach
  void setUp() {
    sessionRepo.deleteAll();
    accountProfileRepo.deleteAll();
    accountRepo.deleteAll();
    ensureDefaultProfile();
    seedParentAccount();
  }

  private void ensureDefaultProfile() {
    if (profileRepo.findById(CurrentProfile.DEFAULT_ID).isEmpty()) {
      var p = new com.sophie.aac.profile.domain.UserProfileEntity();
      p.setId(CurrentProfile.DEFAULT_ID);
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
    ap.setProfileId(CurrentProfile.DEFAULT_ID);
    accountProfileRepo.save(ap);
  }

  @Test
  void login_success_returns_token() {
    AuthService.LoginResult result = authService.login(Role.PARENT, "1234");
    assertThat(result.role()).isEqualTo(Role.PARENT);
    assertThat(result.token()).isNotBlank();
    assertThat(result.ttlMinutes()).isPositive();
    assertThat(result.profileIds()).contains(CurrentProfile.DEFAULT_ID);
    assertThat(result.activeProfileId()).isEqualTo(CurrentProfile.DEFAULT_ID);

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
    assertThat(sessionBefore.getProfileId()).isEqualTo(CurrentProfile.DEFAULT_ID);

    authService.selectProfile(login.token(), CurrentProfile.DEFAULT_ID);
    var sessionAfter = sessionRepo.findAll().get(0);
    assertThat(sessionAfter.getProfileId()).isEqualTo(CurrentProfile.DEFAULT_ID);
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
}
