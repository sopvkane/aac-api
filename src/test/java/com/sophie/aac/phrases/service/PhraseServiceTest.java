package com.sophie.aac.phrases.service;

import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.domain.PhraseNotFoundException;
import com.sophie.aac.phrases.repository.PhraseRepository;
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

  @BeforeEach
  void setUp() {
    repo.deleteAll();
  }

  @Test
  void create_and_list() {
    PhraseEntity created = service.create("Hello", "greeting");
    assertThat(created.getId()).isNotNull();
    assertThat(created.getText()).isEqualTo("Hello");
    assertThat(created.getCategory()).isEqualTo("greeting");

    List<PhraseEntity> list = service.list(Optional.empty(), Optional.empty());
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getText()).isEqualTo("Hello");
  }

  @Test
  void list_with_q_filter() {
    service.create("Hello world", "greeting");
    service.create("I want tea", "needs");

    List<PhraseEntity> filtered = service.list(Optional.of("tea"), Optional.empty());
    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).getText()).isEqualTo("I want tea");
  }

  @Test
  void list_with_category_filter() {
    service.create("Hello", "greeting");
    service.create("I want tea", "needs");

    List<PhraseEntity> filtered = service.list(Optional.empty(), Optional.of("needs"));
    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).getCategory()).isEqualTo("needs");
  }

  @Test
  void get_returns_phrase() {
    PhraseEntity created = service.create("Yes", "affirmation");
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
    PhraseEntity created = service.create("Original", "cat");
    PhraseEntity updated = service.update(created.getId(), "Updated", "new-cat");
    assertThat(updated.getText()).isEqualTo("Updated");
    assertThat(updated.getCategory()).isEqualTo("new-cat");
  }

  @Test
  void delete_removes_phrase() {
    PhraseEntity created = service.create("To delete", "cat");
    service.delete(created.getId());
    assertThat(repo.findById(created.getId())).isEmpty();
  }

  @Test
  void delete_throws_when_not_found() {
    assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
        .isInstanceOf(PhraseNotFoundException.class);
  }
}
