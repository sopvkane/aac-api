package com.sophie.aac.phrases.service;

import com.sophie.aac.auth.util.TestSecurityHelper;
import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.domain.PhraseNotFoundException;
import com.sophie.aac.phrases.repository.PhraseRepository;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:phrase_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class PhraseServiceTest {

  @Autowired
  PhraseService service;

  @Autowired
  PhraseRepository repo;

  @Autowired
  UserProfileRepository profileRepo;

  @BeforeEach
  void setUp() {
    repo.deleteAll();
    ensureDefaultProfile();
    TestSecurityHelper.setParentWithProfile();
  }

  private void ensureDefaultProfile() {
    if (profileRepo.existsById(TestSecurityHelper.DEFAULT_PROFILE_ID)) return;
    UserProfileEntity p = new UserProfileEntity();
    p.setId(TestSecurityHelper.DEFAULT_PROFILE_ID);
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

  @AfterEach
  void tearDown() {
    TestSecurityHelper.clear();
  }

  @Test
  void create_and_list() {
    PhraseEntity created = service.create("Hello", "greeting", null);
    assertThat(created.getId()).isNotNull();
    assertThat(created.getText()).isEqualTo("Hello");
    assertThat(created.getCategory()).isEqualTo("greeting");

    List<PhraseEntity> list = service.list(Optional.empty(), Optional.empty());
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getText()).isEqualTo("Hello");
  }

  @Test
  void list_with_q_filter() {
    service.create("Hello world", "greeting", null);
    service.create("I want tea", "needs", null);

    List<PhraseEntity> filtered = service.list(Optional.of("tea"), Optional.empty());
    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).getText()).isEqualTo("I want tea");
  }

  @Test
  void list_with_category_filter() {
    service.create("Hello", "greeting", null);
    service.create("I want tea", "needs", null);

    List<PhraseEntity> filtered = service.list(Optional.empty(), Optional.of("needs"));
    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).getCategory()).isEqualTo("needs");
  }

  @Test
  void get_returns_phrase() {
    PhraseEntity created = service.create("Yes", "affirmation", null);
    PhraseEntity got = service.get(created.getId());
    assertThat(got.getText()).isEqualTo("Yes");
  }

  @Test
  void get_throws_when_not_found() {
    assertThatThrownBy(() -> service.get(UUID.randomUUID()))
        .isInstanceOf(PhraseNotFoundException.class);
  }

  @Test
  void update_modifies_phrase() {
    PhraseEntity created = service.create("Original", "cat", null);
    PhraseEntity updated = service.update(created.getId(), "Updated", "new-cat", null);
    assertThat(updated.getText()).isEqualTo("Updated");
    assertThat(updated.getCategory()).isEqualTo("new-cat");
  }

  @Test
  void delete_removes_phrase() {
    PhraseEntity created = service.create("To delete", "cat", null);
    service.delete(created.getId());
    assertThat(repo.findById(created.getId())).isEmpty();
  }

  @Test
  void delete_throws_when_not_found() {
    assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
        .isInstanceOf(PhraseNotFoundException.class);
  }
}
