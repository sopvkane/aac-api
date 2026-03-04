package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DialogueSocialReplyBuilder {

  private DialogueSocialReplyBuilder() {
  }

  static DialogueReplyEngine.Result generatePetNameReplies(String qLower, List<PreferenceItemEntity> pets) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    String petType = "pet";
    if (qLower != null && (qLower.contains("dog") || qLower.contains("puppy"))) petType = "dog";
    else if (qLower != null && qLower.contains("cat")) petType = "cat";

    for (PreferenceItemEntity p : pets) {
      if (p == null || p.getLabel() == null || p.getLabel().isBlank()) continue;
      String category = p.getCategory() != null ? p.getCategory().toLowerCase(Locale.ROOT) : "";
      String labelLower = p.getLabel().toLowerCase(Locale.ROOT);
      if (petType.equals("dog") && !category.contains("dog") && !labelLower.contains("dog")) continue;
      if (petType.equals("cat") && !category.contains("cat") && !labelLower.contains("cat")) continue;

      String name = p.getLabel().trim();
      String text = "My " + petType + "'s name is " + name + ".";
      String id = "pet-name-" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      replies.add(new DialogueResponse.Reply(id, name, text, p.getImageUrl(), "PET_NAME"));
      if (replies.size() >= 3) break;
    }

    if (replies.isEmpty()) {
      replies.add(new DialogueResponse.Reply("pet-name-unknown", "I don't know", "I'm not sure.", null, "GENERIC"));
    }
    groups.add(new DialogueResponse.OptionGroup("pets", "Pets", DialogueReplyCommon.labelsOnly(pets)));
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateTeacherNameReplies(List<PreferenceItemEntity> teachers) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    for (PreferenceItemEntity t : teachers) {
      if (t == null || t.getLabel() == null || t.getLabel().isBlank()) continue;
      String name = t.getLabel().trim();
      String text = teachers.size() == 1 ? "My teacher's name is " + name + "." : "One of my teachers is " + name + ".";
      String id = "teacher-" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      replies.add(new DialogueResponse.Reply(id, name, text, t.getImageUrl(), "TEACHER"));
      if (replies.size() >= 3) break;
    }

    if (replies.isEmpty()) {
      replies.add(new DialogueResponse.Reply("teacher-unknown", "I don't know", "I'm not sure.", null, "GENERIC"));
    }
    groups.add(new DialogueResponse.OptionGroup("teachers", "Teachers", DialogueReplyCommon.labelsOnly(teachers)));
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generatePetReplies(String qLower, List<PreferenceItemEntity> pets) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    String asked = DialogueReplyCommon.petTypeFromQuestion(qLower);
    if ("DOG".equals(asked)) {
      replies.add(new DialogueResponse.Reply("pet-yes-dog", "Yes", "Yes, I have a dog.", null, "PETS"));
      replies.add(new DialogueResponse.Reply("pet-no-dog", "No", "No, I don't have a dog.", null, "PETS"));
      replies.add(new DialogueResponse.Reply("pet-other", "Other", "I have a different pet.", null, "PETS"));
    } else if ("CAT".equals(asked)) {
      replies.add(new DialogueResponse.Reply("pet-yes-cat", "Yes", "Yes, I have a cat.", null, "PETS"));
      replies.add(new DialogueResponse.Reply("pet-no-cat", "No", "No, I don't have a cat.", null, "PETS"));
      replies.add(new DialogueResponse.Reply("pet-other", "Other", "I have a different pet.", null, "PETS"));
    } else {
      replies.add(new DialogueResponse.Reply("pet-yes", "Yes", "Yes, I have a pet.", null, "PETS"));
      replies.add(new DialogueResponse.Reply("pet-no", "No", "No, I don't have a pet.", null, "PETS"));
      replies.add(new DialogueResponse.Reply("pet-help", "Help", "Can you help me, please?", null, "GENERIC"));
    }

    List<String> petLabels = pets.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    if (!petLabels.isEmpty()) {
      groups.add(new DialogueResponse.OptionGroup("pets", "Pets", petLabels));
    } else {
      groups.add(new DialogueResponse.OptionGroup("pets", "Pets", List.of("Dog", "Cat", "Fish", "Rabbit")));
    }
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateFamilyReplies(List<PreferenceItemEntity> family) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    replies.add(new DialogueResponse.Reply("fam-mum", "Mum", "Mum.", null, "FAMILY"));
    replies.add(new DialogueResponse.Reply("fam-dad", "Dad", "Dad.", null, "FAMILY"));
    replies.add(new DialogueResponse.Reply("fam-help", "Help", "Can you help me, please?", null, "GENERIC"));

    List<String> people = family.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    if (!people.isEmpty()) {
      groups.add(new DialogueResponse.OptionGroup("family", "People", people));
    } else {
      groups.add(new DialogueResponse.OptionGroup("family", "People", List.of("Mum", "Dad", "Brother", "Sister", "Nan", "Grandad")));
    }
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateFeelingReplies() {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    replies.add(new DialogueResponse.Reply("feel-good", "Good", "I feel good.", null, "CONFIRM"));
    replies.add(new DialogueResponse.Reply("feel-okay", "Okay", "I'm okay.", null, "GENERIC"));
    replies.add(new DialogueResponse.Reply("feel-sad", "Not great", "I don't feel great.", null, "REJECT"));
    return new DialogueReplyEngine.Result(replies, List.of());
  }

  static DialogueReplyEngine.Result generateNameIntroReplies(String qLower) {
    String otherName = extractNameFromIntro(qLower);
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    replies.add(new DialogueResponse.Reply("name-nice", "Nice to meet you", "Nice to meet you!", null, "CONFIRM"));
    replies.add(new DialogueResponse.Reply("name-lovely", "Lovely name", "That's a lovely name.", null, "GENERIC"));
    if (otherName != null && !otherName.isBlank()) {
      String hiText = "Hi " + otherName + "!";
      replies.add(new DialogueResponse.Reply("name-hi", "Hi " + otherName, hiText, null, "GENERIC"));
    } else {
      replies.add(new DialogueResponse.Reply("name-hi", "Hi there", "Hi there!", null, "GENERIC"));
    }
    return new DialogueReplyEngine.Result(replies, List.of());
  }

  private static String extractNameFromIntro(String q) {
    if (q == null || q.isBlank()) return null;
    String s = q.trim();
    String[] patterns = {"my name is ", "i'm ", "i am ", "call me ", "im "};
    for (String p : patterns) {
      int idx = s.toLowerCase(Locale.ROOT).indexOf(p);
      if (idx >= 0) {
        String rest = s.substring(idx + p.length()).trim();
        int end = 0;
        for (int i = 0; i < rest.length(); i++) {
          char c = rest.charAt(i);
          if (Character.isLetter(c) || c == '\'' || c == '-') end = i + 1;
          else break;
        }
        String name = rest.substring(0, end).trim();
        if (name.isEmpty()) return null;
        if (name.length() == 1) return name.toUpperCase(Locale.ROOT);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT);
      }
    }
    return null;
  }
}
