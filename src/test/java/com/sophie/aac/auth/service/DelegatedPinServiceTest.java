package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.domain.UserAccountProfileEntity;
import com.sophie.aac.auth.repository.DelegatedPinRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.util.CurrentProfile;
import com.sophie.aac.auth.util.TestSecurityHelper;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.suggestions.domain.LocationCategory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:delegated_pin_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class DelegatedPinServiceTest {

  @Autowired
  DelegatedPinService delegatedPinService;

  @Autowired
  DelegatedPinRepository delegatedPinRepo;

  @Autowired
  UserAccountProfileRepository userAccountProfileRepo;

  @Autowired
  UserProfileRepository profileRepo;

  @Autowired
  PasswordEncoder encoder;

  private UUID userId;

  @BeforeEach
  void setUp() {
    delegatedPinRepo.deleteAll();
    userAccountProfileRepo.deleteAll();
    ensureDefaultProfile();
    userId = UUID.randomUUID();
    var link = new UserAccountProfileEntity();
    link.setUserId(userId);
    link.setProfileId(CurrentProfile.DEFAULT_ID);
    userAccountProfileRepo.save(link);
  }

  private void ensureDefaultProfile() {
    if (profileRepo.findById(CurrentProfile.DEFAULT_ID).isEmpty()) {
      var p = new UserProfileEntity();
      p.setId(CurrentProfile.DEFAULT_ID);
      p.setDisplayName("Test");
      p.setWakeName("Hey");
      p.setDetailsDefault(true);
      p.setVoiceDefault(false);
      p.setAiEnabled(true);
      p.setMemoryEnabled(true);
      p.setAnalyticsEnabled(false);
      p.setDefaultLocation(LocationCategory.HOME);
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

  @AfterEach
  void tearDown() {
    TestSecurityHelper.clear();
  }

  @Test
  void create_success_saves_pin() {
    TestSecurityHelper.setUserWithProfile(userId);

    DelegatedPinEntity created = delegatedPinService.create("Grandma", "1234", CurrentProfile.DEFAULT_ID);

    assertThat(created.getId()).isNotNull();
    assertThat(created.getLabel()).isEqualTo("Grandma");
    assertThat(created.getProfileId()).isEqualTo(CurrentProfile.DEFAULT_ID);
    assertThat(created.isActive()).isTrue();
    assertThat(encoder.matches("1234", created.getPinHash())).isTrue();
    assertThat(delegatedPinRepo.count()).isEqualTo(1);
  }

  @Test
  void create_unauthorized_when_no_user() {
    assertThatThrownBy(() -> delegatedPinService.create("X", "1234", CurrentProfile.DEFAULT_ID))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("Sign in to create a PIN");
  }

  @Test
  void list_returns_empty_when_no_user() {
    assertThat(delegatedPinService.listByCurrentUser()).isEmpty();
  }

  @Test
  void list_returns_pins_created_by_user() {
    TestSecurityHelper.setUserWithProfile(userId);
    delegatedPinService.create("A", "1111", CurrentProfile.DEFAULT_ID);
    delegatedPinService.create("B", "2222", CurrentProfile.DEFAULT_ID);

    List<DelegatedPinEntity> list = delegatedPinService.listByCurrentUser();

    assertThat(list).hasSize(2);
  }
}
