package com.sophie.aac.wellbeing.service;

import com.sophie.aac.analytics.domain.WellbeingEntryEntity;
import com.sophie.aac.analytics.repository.WellbeingEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:wellbeing_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class WellbeingServiceTest {

  @Autowired
  WellbeingService service;

  @Autowired
  WellbeingEntryRepository repo;

  @BeforeEach
  void setUp() {
    repo.deleteAll();
  }

  @Test
  void recordMood_persists_entry() {
    service.recordMood(5);

    List<WellbeingEntryEntity> all = repo.findAll();
    assertThat(all).hasSize(1);
    assertThat(all.get(0).getMoodScore()).isEqualTo(5);
    assertThat(all.get(0).getSymptomType()).isNull();
  }

  @Test
  void recordPain_persists_entry() {
    service.recordPain("HEAD", 7, "note");

    List<WellbeingEntryEntity> all = repo.findAll();
    assertThat(all).hasSize(1);
    assertThat(all.get(0).getSymptomType()).isEqualTo("PAIN");
    assertThat(all.get(0).getBodyArea()).isEqualTo("HEAD");
    assertThat(all.get(0).getSeverity()).isEqualTo(7);
    assertThat(all.get(0).getNotes()).isEqualTo("note");
  }
}
