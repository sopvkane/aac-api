package com.sophie.aac.analytics.service;

import com.sophie.aac.analytics.domain.InteractionEventEntity;
import com.sophie.aac.analytics.domain.WellbeingEntryEntity;
import com.sophie.aac.analytics.repository.InteractionEventRepository;
import com.sophie.aac.analytics.repository.WellbeingEntryRepository;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.profile.service.CaregiverProfileService;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:dashboard_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class CaregiverDashboardServiceTest {

  @Autowired
  CaregiverDashboardService service;

  @Autowired
  UserProfileRepository profileRepo;

  @Autowired
  InteractionEventRepository interactionRepo;

  @Autowired
  WellbeingEntryRepository wellbeingRepo;

  @BeforeEach
  void setUp() {
    interactionRepo.deleteAll();
    wellbeingRepo.deleteAll();
    ensureDefaultProfile();
  }

  private void ensureDefaultProfile() {
    if (profileRepo.existsById(CaregiverProfileService.DEFAULT_ID)) {
      return;
    }
    UserProfileEntity p = new UserProfileEntity();
    p.setId(CaregiverProfileService.DEFAULT_ID);
    p.setDisplayName("Caregiver");
    p.setWakeName("Hey");
    p.setDetailsDefault(true);
    p.setVoiceDefault(false);
    p.setAiEnabled(true);
    p.setMemoryEnabled(true);
    p.setAnalyticsEnabled(true);
    p.setDefaultLocation(LocationCategory.HOME);
    p.setAllowHome(true);
    p.setAllowSchool(true);
    p.setAllowWork(false);
    p.setAllowOther(true);
    p.setMaxOptions(3);
    p.setFavFood("Apple");
    p.setFavDrink("Juice");
    p.setFavShow("Cartoon");
    p.setFavTopic("Games");
    p.setUpdatedAt(Instant.now());
    profileRepo.save(p);
  }

  @Test
  void getDashboard_returns_response_with_profile_data() {
    CaregiverDashboardResponse resp = service.getDashboard("WEEK");
    assertThat(resp.displayName()).isEqualTo("Caregiver");
    assertThat(resp.favFood()).isEqualTo("Apple");
    assertThat(resp.favDrink()).isEqualTo("Juice");
    assertThat(resp.favShow()).isEqualTo("Cartoon");
    assertThat(resp.period()).isEqualTo("WEEK");
    assertThat(resp.since()).isNotNull();
  }

  @Test
  void getDashboard_includes_interactions() {
    InteractionEventEntity e = new InteractionEventEntity();
    e.setId(UUID.randomUUID());
    e.setEventType("PHRASE_SELECTED");
    e.setLocation(LocationCategory.HOME);
    e.setCreatedAt(Instant.now());
    interactionRepo.save(e);

    CaregiverDashboardResponse resp = service.getDashboard("DAY");
    assertThat(resp.totalInteractionsLast7Days()).isEqualTo(1);
  }

  @Test
  void getDashboard_includes_pain_wellbeing() {
    WellbeingEntryEntity w = new WellbeingEntryEntity();
    w.setId(UUID.randomUUID());
    w.setSymptomType("PAIN");
    w.setBodyArea("head");
    w.setSeverity(5);
    w.setCreatedAt(Instant.now());
    wellbeingRepo.save(w);

    CaregiverDashboardResponse resp = service.getDashboard("WEEK");
    assertThat(resp.wellbeingEntriesLast7Days()).isEqualTo(1);
    assertThat(resp.painEventsLast7Days()).isEqualTo(1);
    assertThat(resp.averagePainSeverityLast7Days()).isEqualTo(5.0);
    assertThat(resp.painByBodyArea()).containsEntry("HEAD", 1L);
  }

  @Test
  void getDashboard_accepts_period_aliases() {
    CaregiverDashboardResponse day = service.getDashboard("1D");
    assertThat(day.period()).isEqualTo("DAY");

    CaregiverDashboardResponse month = service.getDashboard("30D");
    assertThat(month.period()).isEqualTo("MONTH");
  }
}
