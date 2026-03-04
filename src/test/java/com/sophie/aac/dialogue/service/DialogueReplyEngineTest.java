package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DialogueReplyEngineTest {

  @Test
  void filterByLocation_includes_home_and_both_for_home_location() {
    PreferenceItemEntity home = pref("Water", "HOME", "DRINK", null, null);
    PreferenceItemEntity school = pref("Milk", "SCHOOL", "DRINK", null, null);
    PreferenceItemEntity both = pref("Juice", "BOTH", "DRINK", null, null);

    List<PreferenceItemEntity> filtered = DialogueReplyEngine.filterByLocation(List.of(home, school, both), "HOME");

    assertThat(filtered).extracting(PreferenceItemEntity::getLabel)
        .containsExactly("Water", "Juice");
  }

  @Test
  void generateRepliesFromQuestionOptions_extracts_choices() {
    DialogueReplyEngine.Result result = DialogueReplyEngine.generateRepliesFromQuestionOptions(
        "would you like juice or water?", List.of(), List.of(), List.of()
    );

    assertThat(result).isNotNull();
    assertThat(result.replies()).hasSize(2);
    assertThat(result.replies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Juice", "Water");
    assertThat(result.optionGroups()).singleElement()
        .satisfies(group -> assertThat(group.items()).containsExactly("Juice", "Water"));
  }

  @Test
  void ensureRepliesFromOptionsIfNeeded_promotes_option_items_into_tiles() {
    List<DialogueResponse.OptionGroup> groups = List.of(
        new DialogueResponse.OptionGroup("drinks", "Drinks", List.of("Water", "Juice", "Milk"))
    );

    List<DialogueResponse.Reply> replies = DialogueReplyEngine.ensureRepliesFromOptionsIfNeeded(List.of(), groups);

    assertThat(replies).hasSize(3);
    assertThat(replies).extracting(DialogueResponse.Reply::label)
        .containsExactly("Water", "Juice", "Milk");
  }

  @Test
  void ensureExactly3Replies_pads_defaults_when_empty() {
    List<DialogueResponse.Reply> padded = DialogueReplyEngine.ensureExactly3Replies(List.of());

    assertThat(padded).hasSize(3);
    assertThat(padded).extracting(DialogueResponse.Reply::label)
        .containsExactly("Again", "Show me", "Help");
  }

  @Test
  void generateActivityReplies_for_tv_question_uses_fav_show_default() {
    DialogueReplyEngine.Result result = DialogueReplyEngine.generateActivityReplies(
        List.of(), "what is your favourite tv show", "HOME", "Bluey"
    );

    assertThat(result.replies()).hasSize(3);
    assertThat(result.replies().get(0).text()).isEqualTo("My favourite TV show is Bluey.");
    assertThat(result.optionGroups()).singleElement()
        .satisfies(group -> assertThat(group.title()).isEqualTo("TV Shows"));
  }

  @Test
  void generateSchoolReplies_for_what_did_you_do_prioritizes_subjects_and_activities() {
    PreferenceItemEntity maths = pref("Maths", "BOTH", "SUBJECT", null, null);
    PreferenceItemEntity drawing = pref("Drawing", "SCHOOL", "ACTIVITY", null, null);

    DialogueReplyEngine.Result result = DialogueReplyEngine.generateSchoolReplies(
        List.of(maths), List.of(drawing), List.of(), "what did you do at school", "SCHOOL"
    );

    assertThat(result.replies()).isNotEmpty();
    assertThat(result.replies().get(0).text()).isEqualTo("I did Maths.");
    assertThat(result.optionGroups()).singleElement()
        .satisfies(group -> assertThat(group.items()).contains("Maths", "Drawing"));
  }

  @Test
  void generateNameIntroReplies_extracts_name_from_intro() {
    DialogueReplyEngine.Result result = DialogueReplyEngine.generateNameIntroReplies("my name is sophie");

    assertThat(result.replies()).extracting(DialogueResponse.Reply::label)
        .contains("Hi Sophie");
  }

  @Test
  void generateIntroReplies_uses_profile_topic_and_holiday() {
    CaregiverProfileService caregiverProfileService = Mockito.mock(CaregiverProfileService.class);
    DialogueReplyEngine engine = new DialogueReplyEngine(caregiverProfileService);

    UserProfileEntity profile = new UserProfileEntity();
    profile.setFavTopic("Minecraft");
    when(caregiverProfileService.get()).thenReturn(profile);

    PreferenceItemEntity holiday = pref("Beach", "BOTH", "ACTIVITY", "HOLIDAY", null);
    DialogueRequest req = new DialogueRequest("Sophie", "What is your name?", null, null);

    DialogueReplyEngine.Result result = engine.generateIntroReplies(req, List.of(holiday));

    assertThat(result.replies()).hasSize(3);
    assertThat(result.replies().get(0).text())
        .contains("Hello, I am Sophie.")
        .contains("I really like Minecraft.")
        .contains("I like going to Beach.");
  }

  private static PreferenceItemEntity pref(String label, String scope, String kind, String category, String imageUrl) {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setLabel(label);
    e.setScope(scope);
    e.setKind(kind);
    e.setCategory(category);
    e.setImageUrl(imageUrl);
    return e;
  }
}
