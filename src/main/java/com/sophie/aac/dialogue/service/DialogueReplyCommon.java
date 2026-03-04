package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class DialogueReplyCommon {

  private DialogueReplyCommon() {
  }

  static List<PreferenceItemEntity> filterByLocation(List<PreferenceItemEntity> items, String location) {
    if (items == null || items.isEmpty()) return items;
    String loc = (location == null || location.isBlank()) ? "HOME" : location.toUpperCase(Locale.ROOT);
    return items.stream()
        .filter(e -> {
          String scope = e.getScope();
          if (scope == null || scope.isBlank()) return true;
          scope = scope.toUpperCase(Locale.ROOT);
          if ("BOTH".equals(scope)) return true;
          if ("SCHOOL".equals(loc)) return "SCHOOL".equals(scope);
          if ("HOME".equals(loc) || "OUT".equals(loc)) return "HOME".equals(scope);
          return true;
        })
        .toList();
  }

  static List<String> labelsOnly(List<PreferenceItemEntity> entities) {
    if (entities == null || entities.isEmpty()) return List.of();
    List<String> out = new ArrayList<>();
    for (PreferenceItemEntity e : entities) {
      if (e == null) continue;
      String label = e.getLabel();
      if (label != null) {
        String t = label.trim();
        if (!t.isBlank()) out.add(t);
      }
    }
    return out;
  }

  static List<DialogueResponse.OptionItem> toOptionItems(List<PreferenceItemEntity> entities, List<String> labels) {
    if (labels == null || labels.isEmpty()) return List.of();
    List<DialogueResponse.OptionItem> out = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      String label = labels.get(i);
      String iconUrl = (entities != null && i < entities.size() && entities.get(i) != null)
          ? entities.get(i).getImageUrl() : null;
      out.add(new DialogueResponse.OptionItem(label, iconUrl));
    }
    return out;
  }

  static PreferenceItemEntity findPreferenceByLabel(
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> activities,
      String label
  ) {
    if (label == null) return null;
    return subjects.stream().filter(e -> label.equals(e.getLabel())).findFirst()
        .or(() -> activities.stream().filter(e -> label.equals(e.getLabel())).findFirst())
        .orElse(null);
  }

  static List<DialogueResponse.OptionItem> toOptionItemsFromLabels(
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> activities,
      List<String> labels
  ) {
    List<DialogueResponse.OptionItem> items = new ArrayList<>();
    for (String label : labels) {
      PreferenceItemEntity found = findPreferenceByLabel(subjects, activities, label);
      items.add(new DialogueResponse.OptionItem(label, found != null ? found.getImageUrl() : null));
    }
    return items;
  }

  static boolean containsAny(String haystack, String... needles) {
    if (haystack == null || haystack.isBlank()) return false;
    for (String n : needles) {
      if (n != null && !n.isBlank() && haystack.contains(n)) return true;
    }
    return false;
  }

  static boolean isTvShowCategory(String category) {
    if (category == null) return false;
    String c = category.toUpperCase(Locale.ROOT);
    return "TV_SHOW".equals(c) || "TV".equals(c) || "SHOW".equals(c);
  }

  static String petTypeFromQuestion(String qLower) {
    if (qLower == null) return "PET";
    if (qLower.contains("dog") || qLower.contains("puppy")) return "DOG";
    if (qLower.contains("cat")) return "CAT";
    return "PET";
  }

  static String toTitleCase(String s) {
    if (s == null || s.isBlank()) return s;
    return Arrays.stream(s.split("\\s+"))
        .map(w -> w.length() > 1 ? w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1).toLowerCase(Locale.ROOT) : w)
        .collect(Collectors.joining(" "));
  }

  static String displayLabelFor(String rawOption) {
    if (rawOption == null || rawOption.isBlank()) return rawOption;
    String lower = rawOption.toLowerCase(Locale.ROOT);
    if (lower.startsWith("your ")) {
      return toTitleCase(rawOption.substring(5).trim());
    }
    return toTitleCase(rawOption);
  }

  static String withArticle(String label) {
    if (label == null || label.isBlank()) return label;
    String trimmed = label.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (startsWithCountWord(lower) || startsWithNumber(lower)) {
      return trimmed;
    }
    if (lower.contains("juice") || lower.contains("milk") || lower.contains("water") || lower.contains("tea")
        || lower.contains("coffee") || lower.contains("squash") || lower.contains("toast") || lower.contains("bread")
        || lower.contains("pasta") || lower.contains("rice") || lower.contains("cereal")) {
      return trimmed;
    }
    if (trimmed.contains(" ")) {
      int spaceIdx = trimmed.indexOf(' ');
      if (spaceIdx + 1 < trimmed.length() && Character.isUpperCase(trimmed.charAt(spaceIdx + 1))) {
        if (containsAny(lower, "sandwich", "burger", "wrap", "salad", "pizza", "toastie", "roll", "bagel", "muffin", "cookie", "biscuit")) {
          // Continue to article logic.
        } else {
          return trimmed;
        }
      }
    }
    if (lower.startsWith("a") || lower.startsWith("e") || lower.startsWith("i") || lower.startsWith("o")
        || (lower.startsWith("u") && !lower.startsWith("uk"))) {
      return "an " + trimmed;
    }
    return "a " + trimmed;
  }

  private static boolean startsWithCountWord(String lower) {
    int firstSpace = lower.indexOf(' ');
    if (firstSpace <= 0 || firstSpace >= lower.length() - 1) return false;
    String firstWord = lower.substring(0, firstSpace);
    return firstWord.equals("one")
        || firstWord.equals("two")
        || firstWord.equals("three")
        || firstWord.equals("four")
        || firstWord.equals("five")
        || firstWord.equals("six")
        || firstWord.equals("seven")
        || firstWord.equals("eight")
        || firstWord.equals("nine")
        || firstWord.equals("ten");
  }

  private static boolean startsWithNumber(String lower) {
    int i = 0;
    while (i < lower.length() && Character.isDigit(lower.charAt(i))) {
      i++;
    }
    return i > 0 && i < lower.length() && Character.isWhitespace(lower.charAt(i));
  }

  static String phraseWouldLike(String label) {
    if (label == null || label.isBlank()) return "I would like it, please.";
    String trimmed = label.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("your ")) {
      String rest = trimmed.substring(5).trim();
      if (rest.isEmpty()) return "I would like it, please.";
      return "I would like my " + rest + ", please.";
    }
    return "I would like " + withArticle(label) + ", please.";
  }

  static String safe(String s) {
    return s == null ? "" : s.trim();
  }
}
