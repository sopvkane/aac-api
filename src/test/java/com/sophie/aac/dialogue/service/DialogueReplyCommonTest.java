package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueReplyCommonTest {

  @Test
  void filterByLocation_respects_scope_rules() {
    PreferenceItemEntity home = pref("Water", "HOME", null, null);
    PreferenceItemEntity school = pref("Milk", "SCHOOL", null, null);
    PreferenceItemEntity both = pref("Juice", "BOTH", null, null);
    PreferenceItemEntity none = pref("Toast", null, null, null);

    assertThat(DialogueReplyCommon.filterByLocation(List.of(home, school, both, none), "HOME"))
        .extracting(PreferenceItemEntity::getLabel)
        .containsExactly("Water", "Juice", "Toast");
    assertThat(DialogueReplyCommon.filterByLocation(List.of(home, school, both, none), "SCHOOL"))
        .extracting(PreferenceItemEntity::getLabel)
        .containsExactly("Milk", "Juice", "Toast");
    assertThat(DialogueReplyCommon.filterByLocation(List.of(home, school, both, none), "OUT"))
        .extracting(PreferenceItemEntity::getLabel)
        .containsExactly("Water", "Juice", "Toast");
  }

  @Test
  void labels_and_option_items_map_correctly() {
    PreferenceItemEntity water = pref(" Water ", "HOME", "DRINK", "water.png");
    PreferenceItemEntity juice = pref("Juice", "HOME", "DRINK", "juice.png");
    PreferenceItemEntity blank = pref("   ", "HOME", "DRINK", null);

    List<String> labels = DialogueReplyCommon.labelsOnly(List.of(water, blank, juice));
    List<DialogueResponse.OptionItem> optionItems = DialogueReplyCommon.toOptionItems(List.of(water), labels);

    assertThat(labels).containsExactly("Water", "Juice");
    assertThat(optionItems).extracting(DialogueResponse.OptionItem::label).containsExactly("Water", "Juice");
    assertThat(optionItems).extracting(DialogueResponse.OptionItem::iconUrl).containsExactly("water.png", null);
  }

  @Test
  void find_and_map_items_from_labels() {
    PreferenceItemEntity maths = pref("Maths", "SCHOOL", "SUBJECT", "maths.png");
    PreferenceItemEntity drawing = pref("Drawing", "SCHOOL", "ACTIVITY", "draw.png");

    assertThat(DialogueReplyCommon.findPreferenceByLabel(List.of(maths), List.of(drawing), "Drawing"))
        .isEqualTo(drawing);
    assertThat(DialogueReplyCommon.findPreferenceByLabel(List.of(maths), List.of(drawing), "Unknown"))
        .isNull();

    List<DialogueResponse.OptionItem> items = DialogueReplyCommon.toOptionItemsFromLabels(
        List.of(maths), List.of(drawing), List.of("Maths", "Drawing", "Unknown")
    );
    assertThat(items).extracting(DialogueResponse.OptionItem::iconUrl)
        .containsExactly("maths.png", "draw.png", null);
  }

  @Test
  void text_helpers_cover_common_cases() {
    assertThat(DialogueReplyCommon.containsAny("would you like water", "juice", "water")).isTrue();
    assertThat(DialogueReplyCommon.isTvShowCategory("TV_SHOW")).isTrue();
    assertThat(DialogueReplyCommon.isTvShowCategory("GAME")).isFalse();
    assertThat(DialogueReplyCommon.petTypeFromQuestion("do you have a puppy")).isEqualTo("DOG");
    assertThat(DialogueReplyCommon.petTypeFromQuestion("do you have a cat")).isEqualTo("CAT");
    assertThat(DialogueReplyCommon.petTypeFromQuestion("do you have a pet")).isEqualTo("PET");
    assertThat(DialogueReplyCommon.toTitleCase("bluey and BINGO")).isEqualTo("Bluey And Bingo");
    assertThat(DialogueReplyCommon.displayLabelFor("your ipad")).isEqualTo("Ipad");
    assertThat(DialogueReplyCommon.displayLabelFor("hot chocolate")).isEqualTo("Hot Chocolate");

    assertThat(DialogueReplyCommon.withArticle("apple")).isEqualTo("an apple");
    assertThat(DialogueReplyCommon.withArticle("Toast")).isEqualTo("Toast");
    assertThat(DialogueReplyCommon.withArticle("two biscuits")).isEqualTo("two biscuits");
    assertThat(DialogueReplyCommon.phraseWouldLike("your ipad")).isEqualTo("I would like my ipad, please.");
    assertThat(DialogueReplyCommon.phraseWouldLike("pizza")).isEqualTo("I would like a pizza, please.");
    assertThat(DialogueReplyCommon.safe("  hi  ")).isEqualTo("hi");
    assertThat(DialogueReplyCommon.safe(null)).isEqualTo("");
  }

  private static PreferenceItemEntity pref(String label, String scope, String kind, String imageUrl) {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setLabel(label);
    e.setScope(scope);
    e.setKind(kind);
    e.setImageUrl(imageUrl);
    return e;
  }
}
