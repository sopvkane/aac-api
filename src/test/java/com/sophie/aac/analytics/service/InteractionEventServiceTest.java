package com.sophie.aac.analytics.service;

import com.sophie.aac.analytics.domain.InteractionEventEntity;
import com.sophie.aac.analytics.repository.InteractionEventRepository;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InteractionEventServiceTest {

  private InteractionEventRepository repo;
  private AuthContext authContext;
  private InteractionEventService service;

  @BeforeEach
  void setUp() {
    repo = mock(InteractionEventRepository.class);
    authContext = mock(AuthContext.class);
    service = new InteractionEventService(repo, authContext);
  }

  @Test
  void record_applies_defaults_when_inputs_blank() {
    UUID profileId = UUID.randomUUID();
    when(authContext.currentProfileIdOrDefault()).thenReturn(profileId);

    service.record(" ", null, null, null);

    ArgumentCaptor<InteractionEventEntity> captor = ArgumentCaptor.forClass(InteractionEventEntity.class);
    verify(repo).save(captor.capture());
    InteractionEventEntity saved = captor.getValue();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getProfileId()).isEqualTo(profileId);
    assertThat(saved.getEventType()).isEqualTo("OPTION_SELECTED");
    assertThat(saved.getLocation()).isEqualTo(LocationCategory.HOME);
    assertThat(saved.getPromptType()).isNull();
    assertThat(saved.getSelectedText()).isNull();
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void record_truncates_selected_text_to_280_chars() {
    when(authContext.currentProfileIdOrDefault()).thenReturn(UUID.randomUUID());
    String longText = "x".repeat(500);

    service.record("SPOKEN", LocationCategory.SCHOOL, "PROMPT", longText);

    ArgumentCaptor<InteractionEventEntity> captor = ArgumentCaptor.forClass(InteractionEventEntity.class);
    verify(repo).save(captor.capture());
    InteractionEventEntity saved = captor.getValue();

    assertThat(saved.getEventType()).isEqualTo("SPOKEN");
    assertThat(saved.getLocation()).isEqualTo(LocationCategory.SCHOOL);
    assertThat(saved.getPromptType()).isEqualTo("PROMPT");
    assertThat(saved.getSelectedText()).hasSize(280);
  }
}

