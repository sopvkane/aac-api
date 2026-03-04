package com.sophie.aac.profile.service;

import com.sophie.aac.auth.util.TestSecurityHelper;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.profile.web.UpdateUserProfileRequest;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:profile_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class CaregiverProfileServiceTest {

  @Autowired
  CaregiverProfileService service;

  @Autowired
  UserProfileRepository repo;

  @BeforeEach
  void setUp() {
    ensureDefaultProfile();
    TestSecurityHelper.setParentWithProfile();
  }

  @AfterEach
  void tearDown() {
    TestSecurityHelper.clear();
  }

  private void ensureDefaultProfile() {
    if (repo.existsById(CaregiverProfileService.DEFAULT_ID)) {
      return;
    }
    UserProfileEntity p = new UserProfileEntity();
    p.setId(CaregiverProfileService.DEFAULT_ID);
    p.setDisplayName("Test User");
    p.setWakeName("Test");
    p.setDetailsDefault(true);
    p.setVoiceDefault(true);
    p.setAiEnabled(true);
    p.setMemoryEnabled(true);
    p.setAnalyticsEnabled(true);
    p.setDefaultLocation(LocationCategory.HOME);
    p.setAllowHome(true);
    p.setAllowSchool(true);
    p.setAllowWork(false);
    p.setAllowOther(true);
    p.setMaxOptions(3);
    p.setPreferredIconSize("large");
    p.setUpdatedAt(java.time.Instant.now());
    repo.save(p);
  }

  @Test
  void get_returns_profile() {
    UserProfileEntity profile = service.get();
    assertThat(profile.getId()).isEqualTo(CaregiverProfileService.DEFAULT_ID);
    assertThat(profile.getDisplayName()).isEqualTo("Test User");
    assertThat(profile.getWakeName()).isEqualTo("Test");
  }

  @Test
  void get_throws_when_profile_missing() {
    repo.deleteAll();
    assertThatThrownBy(() -> service.get())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Profile not found");
  }

  @Test
  void update_modifies_profile() {
    UpdateUserProfileRequest req = new UpdateUserProfileRequest(
        "Updated Name",
        "Hey You",
        true,
        false,
        true,
        false,
        true,
        LocationCategory.SCHOOL,
        true,
        true,
        true,
        true,
        5,
        "large",
        "Pizza",
        "Water",
        "Show",
        "Topic",
        "About",
        "Mon-Fri",
        "12:00",
        "18:00",
        "21:00",
        "Notes",
        "Class",
        "Teach",
        "Activity"
    );
    UserProfileEntity updated = service.update(req);
    assertThat(updated.getDisplayName()).isEqualTo("Updated Name");
    assertThat(updated.getWakeName()).isEqualTo("Hey You");
    assertThat(updated.getFavFood()).isEqualTo("Pizza");
    assertThat(updated.getFavDrink()).isEqualTo("Water");
    assertThat(updated.getMaxOptions()).isEqualTo(5);
    assertThat(updated.getDefaultLocation()).isEqualTo(LocationCategory.SCHOOL);
    assertThat(updated.getPreferredIconSize()).isEqualTo("large");
  }
}
