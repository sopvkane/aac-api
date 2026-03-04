package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueSemanticEngineTest {

  private DialogueSemanticEngine semanticEngine;

  @BeforeEach
  void setUp() {
    DialogueReplyEngine replyEngine = new DialogueReplyEngine(Mockito.mock(CaregiverProfileService.class));
    semanticEngine = new DialogueSemanticEngine(replyEngine);
  }

  @Test
  void generate_short_circuits_on_or_question_options() {
    DialogueReplyEngine.Result result = semanticEngine.generate(
        DialogueService.Intent.DRINK,
        "would you like juice or water",
        "HOME",
        new DialogueRequest("Sophie", "Would you like juice or water?", null, null),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null
    );

    assertThat(result.replies()).hasSize(3);
    assertThat(result.replies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Juice", "Water", "Help");
  }

  @Test
  void generate_routes_to_offer_and_non_offer_paths() {
    DialogueReplyEngine.Result drinkOffer = semanticEngine.generate(
        DialogueService.Intent.DRINK,
        "are you thirsty",
        "HOME",
        new DialogueRequest("Sophie", "Are you thirsty?", null, null),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null
    );
    DialogueReplyEngine.Result foodNormal = semanticEngine.generate(
        DialogueService.Intent.FOOD,
        "what food do you like",
        "HOME",
        new DialogueRequest("Sophie", "What food do you like?", null, null),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null
    );

    assertThat(drinkOffer.replies()).extracting(DialogueResponse.Reply::id).contains("offer-yes");
    assertThat(foodNormal.replies()).extracting(DialogueResponse.Reply::id).anyMatch(id -> id.startsWith("food-"));
  }

  @Test
  void generate_routes_intro_name_and_default_other() {
    PreferenceItemEntity activity = pref("Drawing", "ACTIVITY");

    DialogueReplyEngine.Result intro = semanticEngine.generate(
        DialogueService.Intent.INTRO_REQUEST,
        "what is your name",
        "HOME",
        new DialogueRequest("Sophie", "What is your name?", null, null),
        List.of(), List.of(), List.of(activity), List.of(), List.of(), List.of(), List.of(), null
    );
    DialogueReplyEngine.Result nameIntro = semanticEngine.generate(
        DialogueService.Intent.NAME_INTRO,
        "my name is alex",
        "HOME",
        new DialogueRequest("Sophie", "My name is Alex", null, null),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null
    );
    DialogueReplyEngine.Result other = semanticEngine.generate(
        DialogueService.Intent.OTHER,
        "hello there",
        "HOME",
        new DialogueRequest("Sophie", "Hello there", null, null),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null
    );

    assertThat(intro.replies()).hasSize(3);
    assertThat(nameIntro.replies()).extracting(DialogueResponse.Reply::label).contains("Hi Alex");
    assertThat(other.optionGroups()).extracting(DialogueResponse.OptionGroup::id)
        .contains("drinks", "foods", "activities");
  }

  private static PreferenceItemEntity pref(String label, String kind) {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setLabel(label);
    e.setKind(kind);
    return e;
  }
}
