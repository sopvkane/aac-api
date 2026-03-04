package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DialoguePreferenceReplyBuilderTest {

  @Test
  void drink_and_food_replies_use_defaults_and_favourite_text() {
    DialogueReplyEngine.Result schoolDrinks = DialoguePreferenceReplyBuilder.generateDrinkReplies(
        List.of(), List.of(), "what is your favourite drink", "SCHOOL"
    );
    DialogueReplyEngine.Result homeFood = DialoguePreferenceReplyBuilder.generateFoodReplies(
        List.of(), List.of(), "what is your favourite food", "HOME"
    );

    assertThat(schoolDrinks.replies().get(0).text()).isEqualTo("My favourite drink is Water.");
    assertThat(schoolDrinks.optionGroups()).hasSize(2);
    assertThat(homeFood.replies().get(0).text()).startsWith("My favourite food is");
    assertThat(homeFood.optionGroups()).hasSize(2);
  }

  @Test
  void school_replies_cover_what_did_and_default_paths() {
    PreferenceItemEntity maths = pref("Maths", "SUBJECT", null, "maths.png");
    PreferenceItemEntity drawing = pref("Drawing", "ACTIVITY", null, "draw.png");
    PreferenceItemEntity teacher = pref("Ms Smith", "TEACHER", null, null);

    DialogueReplyEngine.Result whatDid = DialoguePreferenceReplyBuilder.generateSchoolReplies(
        List.of(maths), List.of(drawing), List.of(teacher), "what did you do at school", "SCHOOL"
    );
    DialogueReplyEngine.Result howWas = DialoguePreferenceReplyBuilder.generateSchoolReplies(
        List.of(), List.of(), List.of(teacher), "how was school", "SCHOOL"
    );

    assertThat(whatDid.replies().get(0).text()).isEqualTo("I did Maths.");
    assertThat(whatDid.optionGroups()).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("subjects"));
    assertThat(howWas.replies()).extracting(DialogueResponse.Reply::label).contains("Good", "Okay", "Not great");
    assertThat(howWas.optionGroups()).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("teachers"));
  }

  @Test
  void activity_replies_cover_tv_game_holiday_and_default_paths() {
    PreferenceItemEntity bluey = pref("Bluey", "ACTIVITY", "TV_SHOW", "bluey.png");
    PreferenceItemEntity minecraft = pref("Minecraft", "ACTIVITY", "GAME", "mc.png");
    PreferenceItemEntity beach = pref("Beach", "ACTIVITY", "HOLIDAY", "beach.png");

    DialogueReplyEngine.Result tv = DialoguePreferenceReplyBuilder.generateActivityReplies(
        List.of(bluey), "what is your favourite tv show", "HOME", "Peppa Pig"
    );
    DialogueReplyEngine.Result gameDefaults = DialoguePreferenceReplyBuilder.generateActivityReplies(
        List.of(), "what game do you like", "HOME", null
    );
    DialogueReplyEngine.Result holiday = DialoguePreferenceReplyBuilder.generateActivityReplies(
        List.of(beach), "what is your favourite holiday place", "HOME", null
    );
    DialogueReplyEngine.Result defaultSchool = DialoguePreferenceReplyBuilder.generateActivityReplies(
        List.of(minecraft), "what do you like to do", "SCHOOL", null
    );

    assertThat(tv.optionGroups().get(0).title()).isEqualTo("TV Shows");
    assertThat(tv.replies().get(0).text()).contains("favourite TV show");
    assertThat(gameDefaults.optionGroups().get(0).items()).contains("Minecraft", "iPad games");
    assertThat(holiday.replies().get(0).label()).isEqualTo("Beach");
    assertThat(defaultSchool.optionGroups().get(0).title()).isEqualTo("Activities");
  }

  @Test
  void generic_preference_and_offer_replies_cover_topic_variants() {
    PreferenceItemEntity water = pref("Water", "DRINK", null, "water.png");
    PreferenceItemEntity toast = pref("Toast", "FOOD", null, "toast.png");
    PreferenceItemEntity draw = pref("Drawing", "ACTIVITY", null, "draw.png");

    DialogueReplyEngine.Result generic = DialoguePreferenceReplyBuilder.generateGenericPreferenceReplies(
        List.of(water), List.of(toast), List.of(draw), "HOME"
    );
    DialogueReplyEngine.Result offerFood = DialoguePreferenceReplyBuilder.generateOfferReplies(
        "food", List.of(water), List.of(toast), List.of(draw), "are you hungry", "HOME"
    );
    DialogueReplyEngine.Result offerDrink = DialoguePreferenceReplyBuilder.generateOfferReplies(
        "drink", List.of(), List.of(), List.of(), "are you thirsty", "SCHOOL"
    );
    DialogueReplyEngine.Result offerActivity = DialoguePreferenceReplyBuilder.generateOfferReplies(
        "activity", List.of(), List.of(), List.of(), "are you ready", "HOME"
    );
    DialogueReplyEngine.Result offerOther = DialoguePreferenceReplyBuilder.generateOfferReplies(
        "other", List.of(), List.of(), List.of(), "do you have your bag", "HOME"
    );

    assertThat(generic.replies()).hasSize(3);
    assertThat(offerFood.replies()).extracting(DialogueResponse.Reply::id).contains("offer-thirsty");
    assertThat(offerDrink.replies()).extracting(DialogueResponse.Reply::id).contains("offer-hungry");
    assertThat(offerActivity.optionGroups()).singleElement().satisfies(g -> assertThat(g.id()).isEqualTo("activities"));
    assertThat(offerOther.replies()).extracting(DialogueResponse.Reply::label).contains("I'm not sure");
  }

  private static PreferenceItemEntity pref(String label, String kind, String category, String imageUrl) {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setLabel(label);
    e.setKind(kind);
    e.setCategory(category);
    e.setImageUrl(imageUrl);
    return e;
  }
}
