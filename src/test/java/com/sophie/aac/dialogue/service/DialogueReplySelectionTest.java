package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueReplySelectionTest {

  @Test
  void generic_and_offer_classifiers_work() {
    List<DialogueResponse.Reply> generic = List.of(
        new DialogueResponse.Reply("a", "Yes", "Yes", null, "GENERIC"),
        new DialogueResponse.Reply("b", "No", "No", null, "GENERIC"),
        new DialogueResponse.Reply("c", "Help", "Help", null, "GENERIC")
    );
    List<DialogueResponse.Reply> specific = List.of(
        new DialogueResponse.Reply("a", "Water", "I would like water", null, "DRINK"),
        new DialogueResponse.Reply("b", "No", "No", null, "GENERIC")
    );

    assertThat(DialogueReplySelection.areRepliesGenericByLabel(generic)).isTrue();
    assertThat(DialogueReplySelection.areRepliesGenericByLabel(specific)).isFalse();
    assertThat(DialogueReplySelection.areRepliesAppropriateForOffer(generic)).isTrue();
    assertThat(DialogueReplySelection.areRepliesAppropriateForOffer(List.of(
        new DialogueResponse.Reply("a", "Water", "I would like water", null, "DRINK")
    ))).isFalse();
  }

  @Test
  void offer_question_detection_handles_positive_and_negative() {
    assertThat(DialogueReplySelection.isOfferQuestion("would you like water")).isTrue();
    assertThat(DialogueReplySelection.isOfferQuestion("are you thirsty")).isTrue();
    assertThat(DialogueReplySelection.isOfferQuestion("what would you like to drink")).isFalse();
    assertThat(DialogueReplySelection.isOfferQuestion("")).isFalse();
  }

  @Test
  void generateRepliesFromQuestionOptions_parses_and_deduplicates() {
    DialogueReplyEngine.Result result = DialogueReplySelection.generateRepliesFromQuestionOptions(
        "would you like apple juice or water or apple juice?"
    );

    assertThat(result).isNotNull();
    assertThat(result.replies()).hasSize(2);
    assertThat(result.replies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Apple Juice", "Water");
    assertThat(DialogueReplySelection.generateRepliesFromQuestionOptions("hello there")).isNull();
  }

  @Test
  void ensureRepliesFromOptionsIfNeeded_and_exactly3_behave_as_expected() {
    List<DialogueResponse.Reply> fromGroups = DialogueReplySelection.ensureRepliesFromOptionsIfNeeded(
        List.of(new DialogueResponse.Reply("x", "Bad", "", null, null)),
        List.of(new DialogueResponse.OptionGroup("d", "Drinks", List.of("Water", "Juice", "Milk")))
    );
    assertThat(fromGroups).hasSize(3);
    assertThat(fromGroups).extracting(DialogueResponse.Reply::label).containsExactly("Water", "Juice", "Milk");

    assertThat(DialogueReplySelection.ensureExactly3Replies(List.of(
        new DialogueResponse.Reply("1", "A", "A", null, null),
        new DialogueResponse.Reply("2", "B", "B", null, null),
        new DialogueResponse.Reply("3", "C", "C", null, null),
        new DialogueResponse.Reply("4", "D", "D", null, null)
    ))).hasSize(3);

    assertThat(DialogueReplySelection.ensureExactly3Replies(List.of())).hasSize(3);
  }

  @Test
  void buildPreferenceOptionGroups_covers_intent_switches() {
    List<PreferenceItemEntity> drinks = List.of(pref("Water", "DRINK"));
    List<PreferenceItemEntity> foods = List.of(pref("Toast", "FOOD"));
    List<PreferenceItemEntity> activities = List.of(pref("Drawing", "ACTIVITY"));
    List<PreferenceItemEntity> subjects = List.of(pref("Maths", "SUBJECT"));
    List<PreferenceItemEntity> pets = List.of(pref("Bella", "PET"));
    List<PreferenceItemEntity> family = List.of(pref("Mum", "FAMILY_MEMBER"));

    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.SCHOOL, drinks, foods, activities, subjects, pets, family
    )).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("subjects"));
    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.DRINK, drinks, foods, activities, subjects, pets, family
    )).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("drinks"));
    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.FOOD, drinks, foods, activities, subjects, pets, family
    )).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("foods"));
    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.ACTIVITIES, drinks, foods, activities, subjects, pets, family
    )).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("activities"));
    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.PETS, drinks, foods, activities, subjects, pets, family
    )).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("pets"));
    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.FAMILY, drinks, foods, activities, subjects, pets, family
    )).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("family"));
    assertThat(DialogueReplySelection.buildPreferenceOptionGroups(
        DialogueService.Intent.OTHER, drinks, foods, activities, subjects, pets, family
    )).hasSize(2);
  }

  private static PreferenceItemEntity pref(String label, String kind) {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setLabel(label);
    e.setKind(kind);
    return e;
  }
}
