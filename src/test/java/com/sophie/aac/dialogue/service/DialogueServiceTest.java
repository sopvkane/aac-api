package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.service.PreferenceItemService;
import com.sophie.aac.profile.service.CaregiverProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DialogueServiceTest {

  @Mock
  AiReplyClient ai;

  @Mock
  UserProfileService userProfileService;

  @Mock
  PreferenceItemService preferenceItemService;

  @Mock
  CaregiverProfileService caregiverProfileService;

  private DialogueReplyEngine replyEngine;
  private DialogueSemanticEngine semanticEngine;

  @BeforeEach
  void setUp() {
    replyEngine = new DialogueReplyEngine(caregiverProfileService);
    semanticEngine = new DialogueSemanticEngine(replyEngine);
    when(userProfileService.loadProfileOrDefault()).thenReturn(defaultProfile());

    when(preferenceItemService.listByKind("DRINK")).thenReturn(List.of(pref("Water", "HOME", "DRINK"), pref("Juice", "BOTH", "DRINK")));
    when(preferenceItemService.listByKind("FOOD")).thenReturn(List.of(pref("Toast", "HOME", "FOOD"), pref("Banana", "BOTH", "FOOD")));
    when(preferenceItemService.listByKind("PET")).thenReturn(List.of(pref("Bella", "BOTH", "PET")));
    when(preferenceItemService.listByKind("TEACHER")).thenReturn(List.of(pref("Ms Smith", "BOTH", "TEACHER")));
    when(preferenceItemService.listByKind("FAMILY_MEMBER")).thenReturn(List.of(pref("Mum", "BOTH", "FAMILY_MEMBER")));
    when(preferenceItemService.listByKind("ACTIVITY")).thenReturn(List.of(pref("Drawing", "SCHOOL", "ACTIVITY"), pref("Bluey", "HOME", "ACTIVITY")));
    when(preferenceItemService.listByKind("SUBJECT")).thenReturn(List.of(pref("Maths", "BOTH", "SUBJECT")));
  }

  @Test
  void semanticMode_uses_question_options_when_or_question_present() {
    DialogueService service = new DialogueService(
        ai, userProfileService, new ObjectMapper(), preferenceItemService, replyEngine, semanticEngine, "SEMANTIC"
    );

    DialogueRequest req = new DialogueRequest("Sophie", "Would you like juice or water?", Map.of("location", "HOME"), null);
    DialogueResponse response = service.generateReplies(req);

    assertThat(response.intent()).isEqualTo("DRINK");
    assertThat(response.topReplies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Juice", "Water", "Help");
    assertThat(response.optionGroups()).isNotEmpty();
    assertThat(response.debug()).containsEntry("llmUsed", false);
  }

  @Test
  void llmMode_falls_back_to_semantic_when_ai_call_fails() {
    DialogueService service = new DialogueService(
        ai, userProfileService, new ObjectMapper(), preferenceItemService, replyEngine, semanticEngine, "LLM"
    );
    when(ai.isConfigured()).thenReturn(true);
    when(ai.generateJson(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

    DialogueRequest req = new DialogueRequest("Sophie", "I am thirsty", Map.of("location", "HOME"), null);
    DialogueResponse response = service.generateReplies(req);

    assertThat(response.intent()).isEqualTo("DRINK");
    assertThat(response.topReplies()).hasSize(3);
    assertThat(response.topReplies().get(0).label()).isEqualTo("Water");
    assertThat(response.debug()).containsEntry("llmUsed", false);
  }

  @Test
  void llmMode_promotes_preference_options_when_llm_returns_generic_replies() {
    DialogueService service = new DialogueService(
        ai, userProfileService, new ObjectMapper(), preferenceItemService, replyEngine, semanticEngine, "LLM"
    );

    when(ai.isConfigured()).thenReturn(true);
    when(ai.generateJson(anyString(), anyString())).thenReturn("""
        {
          "topReplies": [
            {"id":"a","label":"Yes","text":"Yes"},
            {"id":"b","label":"No","text":"No"},
            {"id":"c","label":"Help","text":"Help"}
          ],
          "optionGroups": []
        }
        """);

    DialogueRequest req = new DialogueRequest("Sophie", "What do you want?", Map.of("location", "HOME"), null);
    DialogueResponse response = service.generateReplies(req);

    assertThat(response.intent()).isEqualTo("OTHER");
    assertThat(response.topReplies()).hasSize(3);
    assertThat(response.topReplies()).extracting(DialogueResponse.Reply::label)
        .contains("Water", "Toast");
    assertThat(response.debug()).containsEntry("llmUsed", true);
  }

  @Test
  void hybridMode_uses_semantic_for_non_other_intent_without_llm_override() {
    DialogueService service = new DialogueService(
        ai, userProfileService, new ObjectMapper(), preferenceItemService, replyEngine, semanticEngine, "HYBRID"
    );
    when(ai.isConfigured()).thenReturn(true);

    DialogueRequest req = new DialogueRequest("Sophie", "How are you feeling today?", Map.of("location", "HOME"), null);
    DialogueResponse response = service.generateReplies(req);

    assertThat(response.intent()).isEqualTo("FEELING");
    assertThat(response.topReplies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Good", "Okay", "Not great");
    assertThat(response.debug()).containsEntry("llmUsed", false);
  }

  @Test
  void invalid_mode_defaults_to_semantic() {
    DialogueService service = new DialogueService(
        ai, userProfileService, new ObjectMapper(), preferenceItemService, replyEngine, semanticEngine, "invalid-mode"
    );

    DialogueRequest req = new DialogueRequest("Sophie", "Would you like juice or water?", Map.of("location", "HOME"), null);
    DialogueResponse response = service.generateReplies(req);

    assertThat(response.debug()).containsEntry("dialogueMode", "SEMANTIC");
    assertThat(response.topReplies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Juice", "Water", "Help");
  }

  @Test
  void detects_name_intro_for_im_without_apostrophe() {
    DialogueService service = new DialogueService(
        ai, userProfileService, new ObjectMapper(), preferenceItemService, replyEngine, semanticEngine, "SEMANTIC"
    );

    DialogueRequest req = new DialogueRequest("Sophie", "im alex", Map.of("location", "HOME"), null);
    DialogueResponse response = service.generateReplies(req);

    assertThat(response.intent()).isEqualTo("NAME_INTRO");
  }

  private static UserProfileService.UserProfile defaultProfile() {
    return new UserProfileService.UserProfile(
        "Yes, please. I would like ",
        "No, thank you.",
        "Could you say that again, please?",
        List.of("sandwich"),
        List.of(),
        List.of("water"),
        List.of()
    );
  }

  private static PreferenceItemEntity pref(String label, String scope, String kind) {
    PreferenceItemEntity entity = new PreferenceItemEntity();
    entity.setId(UUID.randomUUID());
    entity.setLabel(label);
    entity.setScope(scope);
    entity.setKind(kind);
    return entity;
  }
}
