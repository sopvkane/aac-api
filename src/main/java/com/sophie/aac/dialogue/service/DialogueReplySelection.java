package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DialogueReplySelection {

  private DialogueReplySelection() {
  }

  static boolean areRepliesGenericByLabel(List<DialogueResponse.Reply> replies) {
    if (replies == null || replies.isEmpty()) return true;
    Set<String> generic = Set.of(
        "yes", "no", "help", "again", "show me", "show me please",
        "yes please", "no thank you", "no thanks", "other"
    );
    int genericCount = 0;
    for (DialogueResponse.Reply r : replies) {
      if (r == null || r.label() == null) continue;
      if (generic.contains(r.label().toLowerCase(Locale.ROOT).trim())) {
        genericCount++;
      }
    }
    return genericCount >= 3;
  }

  static List<DialogueResponse.OptionGroup> buildPreferenceOptionGroups(
      DialogueService.Intent intent,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> pets,
      List<PreferenceItemEntity> family
  ) {
    List<DialogueResponse.OptionGroup> out = new ArrayList<>();
    List<String> labels;
    switch (intent) {
      case SCHOOL -> {
        List<String> subLabels = DialogueReplyCommon.labelsOnly(subjects);
        List<String> actLabels = DialogueReplyCommon.labelsOnly(activities);
        labels = new ArrayList<>(subLabels);
        for (String a : actLabels) {
          if (!labels.contains(a)) labels.add(a);
        }
        if (labels.isEmpty()) labels = List.of("Drawing", "Reading", "Maths");
        out.add(new DialogueResponse.OptionGroup("subjects", "Subjects & activities", labels,
            DialogueReplyCommon.toOptionItemsFromLabels(subjects, activities, labels)));
      }
      case DRINK -> {
        labels = DialogueReplyCommon.labelsOnly(drinks);
        if (labels.isEmpty()) labels = List.of("Water", "Juice", "Milk");
        out.add(new DialogueResponse.OptionGroup("drinks", "Drinks", labels, DialogueReplyCommon.toOptionItems(drinks, labels)));
      }
      case FOOD -> {
        labels = DialogueReplyCommon.labelsOnly(foods);
        if (labels.isEmpty()) labels = List.of("Toast", "Banana", "Sandwich");
        out.add(new DialogueResponse.OptionGroup("foods", "Foods", labels, DialogueReplyCommon.toOptionItems(foods, labels)));
      }
      case ACTIVITIES -> {
        labels = DialogueReplyCommon.labelsOnly(activities);
        if (labels.isEmpty()) labels = List.of("TV", "iPad", "Play", "Outside", "Music", "Drawing");
        out.add(new DialogueResponse.OptionGroup("activities", "Activities", labels, DialogueReplyCommon.toOptionItems(activities, labels)));
      }
      case PETS -> {
        labels = DialogueReplyCommon.labelsOnly(pets);
        if (labels.isEmpty()) labels = List.of("Dog", "Cat", "Fish", "Rabbit");
        out.add(new DialogueResponse.OptionGroup("pets", "Pets", labels, DialogueReplyCommon.toOptionItems(pets, labels)));
      }
      case FAMILY -> {
        labels = DialogueReplyCommon.labelsOnly(family);
        if (labels.isEmpty()) labels = List.of("Mum", "Dad", "Brother", "Sister", "Nan", "Grandad");
        out.add(new DialogueResponse.OptionGroup("family", "People", labels, DialogueReplyCommon.toOptionItems(family, labels)));
      }
      default -> {
        labels = DialogueReplyCommon.labelsOnly(drinks);
        if (labels.isEmpty()) labels = List.of("Water", "Juice", "Milk");
        List<String> foodLabels = DialogueReplyCommon.labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = List.of("Toast", "Banana", "Sandwich");
        out.add(new DialogueResponse.OptionGroup("drinks", "Drinks", labels, DialogueReplyCommon.toOptionItems(drinks, labels)));
        out.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels, DialogueReplyCommon.toOptionItems(foods, foodLabels)));
      }
    }
    return out;
  }

  static boolean areRepliesAppropriateForOffer(List<DialogueResponse.Reply> replies) {
    if (replies == null || replies.size() < 2) return false;
    Set<String> offerLike = Set.of(
        "yes", "no", "yes please", "no thank you", "no thanks",
        "i'm thirsty", "im thirsty", "i'm hungry", "im hungry",
        "drink instead", "food instead", "something else", "help"
    );
    int match = 0;
    for (DialogueResponse.Reply r : replies) {
      if (r == null || r.label() == null) continue;
      String lbl = r.label().toLowerCase(Locale.ROOT).trim();
      if (offerLike.contains(lbl) || lbl.startsWith("i'm ") || lbl.startsWith("im ")) {
        match++;
      }
    }
    return match >= 2;
  }

  static boolean isOfferQuestion(String qLower) {
    if (qLower == null || qLower.isBlank()) return false;
    if (qLower.contains("what would you like") || qLower.contains("what do you want")
        || qLower.contains("which would you like") || qLower.contains("which do you want")) {
      return false;
    }
    return qLower.contains("would you like")
        || qLower.contains("do you want")
        || qLower.contains("do you have")
        || qLower.contains("are you hungry")
        || qLower.contains("are you thirsty")
        || qLower.contains("are you tired")
        || qLower.contains("are you excited")
        || qLower.contains("are you ready")
        || qLower.contains("ready to go")
        || qLower.contains("ready for bed")
        || qLower.contains("ready to brush")
        || qLower.contains("would you like a")
        || qLower.contains("would you like some")
        || qLower.contains("do you want a")
        || qLower.contains("do you want some");
  }

  static DialogueReplyEngine.Result generateRepliesFromQuestionOptions(String qLower) {
    if (qLower == null || qLower.isBlank()) return null;
    if (!qLower.contains(" or ")) return null;

    Set<String> fillerWords = Set.of("instead", "else", "please", "thanks", "then", "too");
    String[] parts = qLower.split("\\s+or\\s+");
    if (parts.length < 2) return null;

    List<String> options = new ArrayList<>();
    for (String part : parts) {
      String p = part.replaceAll("[?.,!]", "").trim();
      if (p.isBlank()) continue;

      p = p.replaceFirst("^(would you like|do you want|can i get you)\\s+", "");
      p = p.replaceFirst("^(a|some)\\s+", "");
      if (p.contains(" of ")) {
        p = p.replaceFirst("\\s+of\\s+\\w+$", "");
      }
      p = p.trim();

      String[] words = p.split("\\s+");
      String candidate;
      if (words.length >= 2) {
        String lastWord = words[words.length - 1].toLowerCase(Locale.ROOT);
        if (fillerWords.contains(lastWord)) {
          candidate = words[words.length - 2];
        } else {
          candidate = words[words.length - 2] + " " + words[words.length - 1];
        }
      } else {
        candidate = words.length > 0 ? words[words.length - 1] : "";
      }
      if (candidate.length() >= 2 && !fillerWords.contains(candidate.toLowerCase(Locale.ROOT).split("\\s+")[0])) {
        options.add(candidate);
      }
    }

    if (options.isEmpty()) return null;

    List<String> deduped = new ArrayList<>();
    List<String> rawOpts = new ArrayList<>();
    for (String opt : options) {
      String key = opt.toLowerCase(Locale.ROOT);
      if (deduped.stream().anyMatch(s -> s.equalsIgnoreCase(key))) continue;
      deduped.add(opt);
      rawOpts.add(opt);
    }

    if (deduped.isEmpty()) return null;

    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();
    for (String opt : rawOpts) {
      String displayLabel = DialogueReplyCommon.displayLabelFor(opt);
      String id = "opt-" + opt.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      replies.add(new DialogueResponse.Reply(id, displayLabel, DialogueReplyCommon.phraseWouldLike(opt), null, null));
      if (replies.size() >= 3) break;
    }
    List<String> displayLabels = rawOpts.stream().map(DialogueReplyCommon::displayLabelFor).toList();
    groups.add(new DialogueResponse.OptionGroup("choices", "Your choices", displayLabels));
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static List<DialogueResponse.Reply> ensureRepliesFromOptionsIfNeeded(
      List<DialogueResponse.Reply> replies,
      List<DialogueResponse.OptionGroup> optionGroups
  ) {
    List<DialogueResponse.Reply> out = new ArrayList<>();
    if (replies != null) {
      for (DialogueResponse.Reply r : replies) {
        if (r != null && r.text() != null && !r.text().isBlank()) {
          out.add(r);
        }
      }
    }

    if (out.size() >= 3) {
      return out;
    }

    if (optionGroups == null || optionGroups.isEmpty()) {
      return out;
    }

    for (DialogueResponse.OptionGroup g : optionGroups) {
      if (g == null || g.items() == null || g.items().isEmpty()) continue;
      for (String item : g.items()) {
        if (out.size() >= 3) break;
        if (item == null || item.isBlank()) continue;
        String label = item.trim();
        String text = DialogueReplyCommon.phraseWouldLike(label);
        String id = "opt-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        out.add(new DialogueResponse.Reply(id, label, text, null, null));
      }
      if (out.size() >= 3) break;
    }

    return out;
  }

  static List<DialogueResponse.Reply> ensureExactly3Replies(List<DialogueResponse.Reply> in) {
    List<DialogueResponse.Reply> out = new ArrayList<>();
    if (in != null) out.addAll(in);

    if (out.size() > 3) {
      return out.subList(0, 3);
    }

    while (out.size() < 3) {
      int idx = out.size() + 1;
      if (idx == 1) {
        out.add(new DialogueResponse.Reply("r1", "Again", "Can you say it again, please?", null, "GENERIC"));
      } else if (idx == 2) {
        out.add(new DialogueResponse.Reply("r2", "Show me", "Can you show me, please?", null, "GENERIC"));
      } else {
        out.add(new DialogueResponse.Reply("r3", "Help", "Can you help me, please?", null, "GENERIC"));
      }
    }

    return out;
  }
}
