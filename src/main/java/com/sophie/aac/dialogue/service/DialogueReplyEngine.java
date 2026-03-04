package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DialogueReplyEngine {

  private static final Logger log = LoggerFactory.getLogger(DialogueReplyEngine.class);
  private final CaregiverProfileService caregiverProfileService;

  DialogueReplyEngine(CaregiverProfileService caregiverProfileService) {
    this.caregiverProfileService = caregiverProfileService;
  }

  record Result(List<DialogueResponse.Reply> replies, List<DialogueResponse.OptionGroup> optionGroups) {
  }

  String getFavShowOrNull() {
    try {
      return caregiverProfileService.get().getFavShow();
    } catch (Exception ex) {
      log.debug("Could not resolve caregiver fav show; continuing without profile-derived fallback.", ex);
      return null;
    }
  }

  static List<PreferenceItemEntity> filterByLocation(List<PreferenceItemEntity> items, String location) {
    return DialogueReplyCommon.filterByLocation(items, location);
  }

  static Result generatePetNameReplies(String qLower, List<PreferenceItemEntity> pets) {
    return DialogueSocialReplyBuilder.generatePetNameReplies(qLower, pets);
  }

  static Result generateTeacherNameReplies(List<PreferenceItemEntity> teachers) {
    return DialogueSocialReplyBuilder.generateTeacherNameReplies(teachers);
  }

  static Result generatePetReplies(String qLower, List<PreferenceItemEntity> pets) {
    return DialogueSocialReplyBuilder.generatePetReplies(qLower, pets);
  }

  static Result generateDrinkReplies(List<PreferenceItemEntity> drinks, List<PreferenceItemEntity> foods, String qLower, String location) {
    return DialoguePreferenceReplyBuilder.generateDrinkReplies(drinks, foods, qLower, location);
  }

  static Result generateFoodReplies(List<PreferenceItemEntity> drinks, List<PreferenceItemEntity> foods, String qLower, String location) {
    return DialoguePreferenceReplyBuilder.generateFoodReplies(drinks, foods, qLower, location);
  }

  static Result generateFamilyReplies(List<PreferenceItemEntity> family) {
    return DialogueSocialReplyBuilder.generateFamilyReplies(family);
  }

  static Result generateSchoolReplies(
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> teachers,
      String qLower,
      String location
  ) {
    return DialoguePreferenceReplyBuilder.generateSchoolReplies(subjects, activities, teachers, qLower, location);
  }

  static Result generateFeelingReplies() {
    return DialogueSocialReplyBuilder.generateFeelingReplies();
  }

  Result generateIntroReplies(DialogueRequest req, List<PreferenceItemEntity> activities) {
    String name = DialogueReplyCommon.safe(req.userName());
    if (name == null || name.isBlank()) name = "I";

    String favGame = null;
    String holiday = null;
    try {
      UserProfileEntity profile = caregiverProfileService.get();
      favGame = profile.getFavTopic();
      if (favGame == null || favGame.isBlank()) favGame = profile.getFavShow();
      List<PreferenceItemEntity> holidayActivities = activities.stream()
          .filter(e -> e != null && e.getCategory() != null && "HOLIDAY".equalsIgnoreCase(e.getCategory()))
          .filter(e -> e.getLabel() != null && !e.getLabel().isBlank())
          .toList();
      if (!holidayActivities.isEmpty()) {
        holiday = holidayActivities.get(0).getLabel().trim();
      }
    } catch (Exception ex) {
      log.debug("Could not resolve caregiver profile for intro reply; using generic intro.", ex);
    }

    StringBuilder intro = new StringBuilder();
    intro.append("Hello, I am ").append(name).append(".");
    if (favGame != null && !favGame.isBlank()) {
      intro.append(" I really like ").append(favGame).append(".");
    }
    if (holiday != null && !holiday.isBlank()) {
      intro.append(" I like going to ").append(holiday).append(".");
    }
    intro.append(" What is your name?");

    List<DialogueResponse.Reply> replies = new ArrayList<>();
    replies.add(new DialogueResponse.Reply("intro-full", "Introduce myself", intro.toString(), null, "INTRO"));
    replies.add(new DialogueResponse.Reply("intro-ask", "What is your name?", "What is your name?", null, "GENERIC"));
    replies.add(new DialogueResponse.Reply("intro-hi", "Hello", "Hello!", null, "GENERIC"));
    return new Result(replies, List.of());
  }

  static Result generateNameIntroReplies(String qLower) {
    return DialogueSocialReplyBuilder.generateNameIntroReplies(qLower);
  }

  static Result generateActivityReplies(List<PreferenceItemEntity> activities, String qLower, String location, String favShowFromProfile) {
    return DialoguePreferenceReplyBuilder.generateActivityReplies(activities, qLower, location, favShowFromProfile);
  }

  static Result generateGenericPreferenceReplies(
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      String location
  ) {
    return DialoguePreferenceReplyBuilder.generateGenericPreferenceReplies(drinks, foods, activities, location);
  }

  static boolean areRepliesGenericByLabel(List<DialogueResponse.Reply> replies) {
    return DialogueReplySelection.areRepliesGenericByLabel(replies);
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
    return DialogueReplySelection.buildPreferenceOptionGroups(intent, drinks, foods, activities, subjects, pets, family);
  }

  static boolean areRepliesAppropriateForOffer(List<DialogueResponse.Reply> replies) {
    return DialogueReplySelection.areRepliesAppropriateForOffer(replies);
  }

  static boolean isOfferQuestion(String qLower) {
    return DialogueReplySelection.isOfferQuestion(qLower);
  }

  static Result generateRepliesFromQuestionOptions(
      String qLower,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities
  ) {
    return DialogueReplySelection.generateRepliesFromQuestionOptions(qLower);
  }

  static Result generateOfferReplies(
      String topic,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      String qLower,
      String location
  ) {
    return DialoguePreferenceReplyBuilder.generateOfferReplies(topic, drinks, foods, activities, qLower, location);
  }

  static List<DialogueResponse.Reply> ensureRepliesFromOptionsIfNeeded(
      List<DialogueResponse.Reply> replies,
      List<DialogueResponse.OptionGroup> optionGroups
  ) {
    return DialogueReplySelection.ensureRepliesFromOptionsIfNeeded(replies, optionGroups);
  }

  static List<DialogueResponse.Reply> ensureExactly3Replies(List<DialogueResponse.Reply> in) {
    return DialogueReplySelection.ensureExactly3Replies(in);
  }

  static List<String> labelsOnly(List<PreferenceItemEntity> entities) {
    return DialogueReplyCommon.labelsOnly(entities);
  }
}
