package com.sophie.aac.preferences.service;

import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.repository.PreferenceItemRepository;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
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

  @BeforeEach
  void setUp() {
    repo.deleteAll();
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
}
