package com.sophie.aac.preferences.service;

import com.sophie.aac.auth.util.TestSecurityHelper;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.repository.PreferenceItemRepository;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:pref_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class PreferenceItemServiceTest {

  @Autowired
  PreferenceItemService service;

  @Autowired
  PreferenceItemRepository repo;

  @Autowired
  UserProfileRepository profileRepo;

  @BeforeEach
  void setUp() {
    repo.deleteAll();
    ensureDefaultProfile();
    TestSecurityHelper.setParentWithProfile();
  }

  @AfterEach
  void tearDown() {
    TestSecurityHelper.clear();
  }

  private void ensureDefaultProfile() {
    if (profileRepo.findById(TestSecurityHelper.DEFAULT_PROFILE_ID).isEmpty()) {
      UserProfileEntity p = new UserProfileEntity();
      p.setId(TestSecurityHelper.DEFAULT_PROFILE_ID);
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

  @Test
  void listByKind_returns_empty_when_none() {
    List<PreferenceItemEntity> list = service.listByKind("EMOTION");
    assertThat(list).isEmpty();
  }

  @Test
  void create_and_listByKind() {
    PreferenceItemRequest req = new PreferenceItemRequest("emotion", "Happy", "feelings", null, null, "user", 10);
    PreferenceItemEntity created = service.create(req, "PARENT");
    assertThat(created.getId()).isNotNull();
    assertThat(created.getKind()).isEqualTo("EMOTION");
    assertThat(created.getLabel()).isEqualTo("Happy");
    assertThat(created.getCategory()).isEqualTo("feelings");
    assertThat(created.getScope()).isEqualTo("USER");
    assertThat(created.getPriority()).isEqualTo(10);
    assertThat(created.getCreatedByRole()).isEqualTo("PARENT");

    List<PreferenceItemEntity> list = service.listByKind("EMOTION");
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getLabel()).isEqualTo("Happy");
  }

  @Test
  void update_modifies_item() {
    PreferenceItemRequest createReq = new PreferenceItemRequest("emotion", "Sad", null, null, null, "user", 5);
    PreferenceItemEntity created = service.create(createReq, "PARENT");

    PreferenceItemRequest updateReq = new PreferenceItemRequest("emotion", "Joy", "positive", null, null, "user", 20);
    PreferenceItemEntity updated = service.update(created.getId(), updateReq);
    assertThat(updated.getLabel()).isEqualTo("Joy");
    assertThat(updated.getCategory()).isEqualTo("positive");
    assertThat(updated.getPriority()).isEqualTo(20);
  }

  @Test
  void update_throws_when_not_found() {
    PreferenceItemRequest req = new PreferenceItemRequest("emotion", "X", null, null, null, "user", null);
    assertThatThrownBy(() -> service.update(UUID.randomUUID(), req))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void delete_removes_item() {
    PreferenceItemRequest req = new PreferenceItemRequest("emotion", "Angry", null, null, null, "user", null);
    PreferenceItemEntity created = service.create(req, "PARENT");
    service.delete(created.getId());
    assertThat(repo.findById(created.getId())).isEmpty();
  }

  @Test
  void listWhoToAskByLocation_HOME_returns_only_family() {
    service.create(new PreferenceItemRequest("FAMILY_MEMBER", "Mum", null, null, null, "HOME", 10), "PARENT");
    service.create(new PreferenceItemRequest("FAMILY_MEMBER", "Dad", null, null, null, "HOME", 9), "PARENT");
    service.create(new PreferenceItemRequest("TEACHER", "Mrs Patel", null, null, null, "SCHOOL", 10), "PARENT");
    service.create(new PreferenceItemRequest("BUS_STAFF", "Dave", null, null, null, "SCHOOL", 10), "PARENT");

    List<PreferenceItemEntity> result = service.listWhoToAskByLocation("HOME");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(PreferenceItemEntity::getKind).containsOnly("FAMILY_MEMBER");
    assertThat(result).extracting(PreferenceItemEntity::getLabel).containsExactlyInAnyOrder("Mum", "Dad");
  }

  @Test
  void listWhoToAskByLocation_SCHOOL_returns_only_teachers() {
    service.create(new PreferenceItemRequest("FAMILY_MEMBER", "Mum", null, null, null, "HOME", 10), "PARENT");
    service.create(new PreferenceItemRequest("TEACHER", "Mrs Patel", null, null, null, "SCHOOL", 10), "PARENT");
    service.create(new PreferenceItemRequest("TEACHER", "Mr Jones", null, null, null, "SCHOOL", 9), "PARENT");
    service.create(new PreferenceItemRequest("BUS_STAFF", "Dave (driver)", null, null, null, "SCHOOL", 10), "PARENT");
    service.create(new PreferenceItemRequest("BUS_STAFF", "Sarah (assistant)", null, null, null, "SCHOOL", 9), "PARENT");

    List<PreferenceItemEntity> result = service.listWhoToAskByLocation("SCHOOL");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(PreferenceItemEntity::getKind).containsOnly("TEACHER");
    assertThat(result).extracting(PreferenceItemEntity::getLabel).containsExactlyInAnyOrder("Mrs Patel", "Mr Jones");
  }

  @Test
  void listWhoToAskByLocation_BUS_returns_only_bus_staff() {
    service.create(new PreferenceItemRequest("FAMILY_MEMBER", "Mum", null, null, null, "HOME", 10), "PARENT");
    service.create(new PreferenceItemRequest("TEACHER", "Mrs Patel", null, null, null, "SCHOOL", 10), "PARENT");
    service.create(new PreferenceItemRequest("BUS_STAFF", "Dave (driver)", null, null, null, "SCHOOL", 10), "PARENT");
    service.create(new PreferenceItemRequest("BUS_STAFF", "Sarah (assistant)", null, null, null, "SCHOOL", 9), "PARENT");

    List<PreferenceItemEntity> result = service.listWhoToAskByLocation("BUS");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(PreferenceItemEntity::getKind).containsOnly("BUS_STAFF");
    assertThat(result).extracting(PreferenceItemEntity::getLabel).containsExactlyInAnyOrder("Dave (driver)", "Sarah (assistant)");
  }

  @Test
  void listWhoToAskByLocation_returns_empty_when_no_matching_items() {
    service.create(new PreferenceItemRequest("TEACHER", "Mrs Patel", null, null, null, "SCHOOL", 10), "PARENT");

    assertThat(service.listWhoToAskByLocation("HOME")).isEmpty();
    assertThat(service.listWhoToAskByLocation("BUS")).isEmpty();
  }

  @Test
  void listWhoToAskByLocation_includes_BOTH_scope() {
    service.create(new PreferenceItemRequest("FAMILY_MEMBER", "Grandma", null, null, null, "BOTH", 5), "PARENT");

    List<PreferenceItemEntity> result = service.listWhoToAskByLocation("HOME");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLabel()).isEqualTo("Grandma");
  }
}
