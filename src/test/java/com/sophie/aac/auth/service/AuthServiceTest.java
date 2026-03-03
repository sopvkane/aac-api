package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.AuthSessionEntity;
import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.AuthSessionRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
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
  PasswordEncoder encoder;

  @BeforeEach
  void setUp() {
    sessionRepo.deleteAll();
    accountRepo.deleteAll();
    seedParentAccount();
  }

  private void seedParentAccount() {
    CaregiverAccountEntity acc = new CaregiverAccountEntity();
    acc.setId(java.util.UUID.randomUUID());
    acc.setRole(Role.PARENT);
    acc.setPinHash(encoder.encode("1234"));
    acc.setActive(true);
    acc.setCreatedAt(java.time.Instant.now());
    accountRepo.save(acc);
  }

  @Test
  void login_success_returns_token() {
    AuthService.LoginResult result = authService.login(Role.PARENT, "1234");
    assertThat(result.role()).isEqualTo(Role.PARENT);
    assertThat(result.token()).isNotBlank();
    assertThat(result.ttlMinutes()).isPositive();

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
}
