package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueSocialReplyBuilderTest {

  @Test
  void generatePetNameReplies_filters_by_pet_type_and_has_fallback() {
    PreferenceItemEntity dog = pref("Rex", "PET_DOG", null);
    PreferenceItemEntity cat = pref("Milo", "PET_CAT", null);

    DialogueReplyEngine.Result dogResult = DialogueSocialReplyBuilder.generatePetNameReplies("what is your dog name", List.of(dog, cat));
    DialogueReplyEngine.Result noneResult = DialogueSocialReplyBuilder.generatePetNameReplies("what is your cat name", List.of(dog));

    assertThat(dogResult.replies()).extracting(DialogueResponse.Reply::label).contains("Rex");
    assertThat(noneResult.replies()).singleElement().satisfies(r -> assertThat(r.label()).isEqualTo("I don't know"));
  }

  @Test
  void generateTeacherNameReplies_uses_single_and_multiple_texts() {
    DialogueReplyEngine.Result single = DialogueSocialReplyBuilder.generateTeacherNameReplies(List.of(pref("Ms Smith", "TEACHER", null)));
    DialogueReplyEngine.Result multiple = DialogueSocialReplyBuilder.generateTeacherNameReplies(List.of(
        pref("Ms Smith", "TEACHER", null),
        pref("Mr Jones", "TEACHER", null)
    ));

    assertThat(single.replies().get(0).text()).contains("My teacher's name is");
    assertThat(multiple.replies().get(0).text()).contains("One of my teachers is");
  }

  @Test
  void generatePetReplies_family_and_feeling_replies_have_expected_defaults() {
    DialogueReplyEngine.Result petDog = DialogueSocialReplyBuilder.generatePetReplies("do you have a dog", List.of());
    DialogueReplyEngine.Result petDefault = DialogueSocialReplyBuilder.generatePetReplies("do you have pets", List.of());
    DialogueReplyEngine.Result family = DialogueSocialReplyBuilder.generateFamilyReplies(List.of());
    DialogueReplyEngine.Result feelings = DialogueSocialReplyBuilder.generateFeelingReplies();

    assertThat(petDog.replies()).extracting(DialogueResponse.Reply::id).contains("pet-yes-dog");
    assertThat(petDefault.optionGroups()).singleElement()
        .satisfies(g -> assertThat(g.items()).contains("Dog", "Cat", "Fish", "Rabbit"));
    assertThat(family.optionGroups()).singleElement()
        .satisfies(g -> assertThat(g.items()).contains("Mum", "Dad"));
    assertThat(feelings.replies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Good", "Okay", "Not great");
  }

  @Test
  void generateNameIntroReplies_extracts_multiple_intro_patterns() {
    assertThat(DialogueSocialReplyBuilder.generateNameIntroReplies("my name is alex").replies())
        .extracting(DialogueResponse.Reply::label).contains("Hi Alex");
    assertThat(DialogueSocialReplyBuilder.generateNameIntroReplies("im sam").replies())
        .extracting(DialogueResponse.Reply::label).contains("Hi Sam");
    assertThat(DialogueSocialReplyBuilder.generateNameIntroReplies("hello").replies())
        .extracting(DialogueResponse.Reply::label).contains("Hi there");
  }

  private static PreferenceItemEntity pref(String label, String category, String imageUrl) {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setLabel(label);
    e.setCategory(category);
    e.setImageUrl(imageUrl);
    return e;
  }
}
