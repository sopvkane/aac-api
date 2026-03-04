package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class DialogueSemanticEngine {

  private final DialogueReplyEngine replyEngine;

  DialogueSemanticEngine(DialogueReplyEngine replyEngine) {
    this.replyEngine = replyEngine;
  }

  DialogueReplyEngine.Result generate(
      DialogueService.Intent intent,
      String qLower,
      String location,
      DialogueRequest req,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> pets,
      List<PreferenceItemEntity> teachers,
      List<PreferenceItemEntity> family,
      String favShow
  ) {
    DialogueReplyEngine.Result fromQuestion = DialogueReplyEngine.generateRepliesFromQuestionOptions(qLower, drinks, foods, activities);
    if (fromQuestion != null && !fromQuestion.replies().isEmpty()) {
      return new DialogueReplyEngine.Result(
          DialogueReplyEngine.ensureExactly3Replies(fromQuestion.replies()),
          fromQuestion.optionGroups()
      );
    }

    boolean isOffer = DialogueReplyEngine.isOfferQuestion(qLower);
    return switch (intent) {
      case PET_NAME -> DialogueReplyEngine.generatePetNameReplies(qLower, pets);
      case TEACHER_NAME -> DialogueReplyEngine.generateTeacherNameReplies(teachers);
      case PETS -> DialogueReplyEngine.generatePetReplies(qLower, pets);
      case DRINK -> isOffer
          ? DialogueReplyEngine.generateOfferReplies("drink", drinks, foods, List.of(), qLower, location)
          : DialogueReplyEngine.generateDrinkReplies(drinks, foods, qLower, location);
      case FOOD -> isOffer
          ? DialogueReplyEngine.generateOfferReplies("food", drinks, foods, List.of(), qLower, location)
          : DialogueReplyEngine.generateFoodReplies(drinks, foods, qLower, location);
      case FAMILY -> DialogueReplyEngine.generateFamilyReplies(family);
      case ACTIVITIES -> isOffer
          ? DialogueReplyEngine.generateOfferReplies("activity", drinks, foods, activities, qLower, location)
          : DialogueReplyEngine.generateActivityReplies(activities, qLower, location, favShow);
      case SCHOOL -> DialogueReplyEngine.generateSchoolReplies(subjects, activities, teachers, qLower, location);
      case FEELING -> DialogueReplyEngine.generateFeelingReplies();
      case INTRO_REQUEST -> replyEngine.generateIntroReplies(req, activities);
      case NAME_INTRO -> DialogueReplyEngine.generateNameIntroReplies(qLower);
      default -> isOffer
          ? DialogueReplyEngine.generateOfferReplies("other", drinks, foods, List.of(), qLower, location)
          : DialogueReplyEngine.generateGenericPreferenceReplies(drinks, foods, activities, location);
    };
  }
}
