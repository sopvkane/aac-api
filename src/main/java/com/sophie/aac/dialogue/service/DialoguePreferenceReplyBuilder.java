package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DialoguePreferenceReplyBuilder {

  private DialoguePreferenceReplyBuilder() {
  }

  static DialogueReplyEngine.Result generateDrinkReplies(List<PreferenceItemEntity> drinks, List<PreferenceItemEntity> foods, String qLower, String location) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    List<String> drinkLabels = drinks.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    List<String> foodLabels = foods.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();

    if (drinkLabels.isEmpty()) {
      drinkLabels = "SCHOOL".equalsIgnoreCase(location)
          ? List.of("Water")
          : List.of("Water", "Juice", "Milk");
    }
    if (foodLabels.isEmpty()) {
      foodLabels = List.of("Toast", "Banana", "Sandwich");
    }

    groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels, DialogueReplyCommon.toOptionItems(drinks, drinkLabels)));
    groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels, DialogueReplyCommon.toOptionItems(foods, foodLabels)));

    boolean isFavourite = qLower != null && qLower.contains("favourite");

    for (int i = 0; i < Math.min(3, drinkLabels.size()); i++) {
      String label = drinkLabels.get(i);
      String iconUrl = i < drinks.size() ? drinks.get(i).getImageUrl() : null;
      String id = "drink-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      String text = isFavourite ? "My favourite drink is " + label + "." : DialogueReplyCommon.phraseWouldLike(label);
      replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "DRINK"));
    }
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateFoodReplies(List<PreferenceItemEntity> drinks, List<PreferenceItemEntity> foods, String qLower, String location) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    List<String> foodLabels = foods.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    List<String> drinkLabels = drinks.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();

    if (foodLabels.isEmpty()) {
      foodLabels = "SCHOOL".equalsIgnoreCase(location)
          ? List.of("Sandwich", "Fruit", "Crackers")
          : List.of("Toast", "Banana", "Sandwich");
    }
    if (drinkLabels.isEmpty()) {
      drinkLabels = "SCHOOL".equalsIgnoreCase(location)
          ? List.of("Water")
          : List.of("Water", "Juice", "Milk");
    }

    groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels, DialogueReplyCommon.toOptionItems(foods, foodLabels)));
    groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels, DialogueReplyCommon.toOptionItems(drinks, drinkLabels)));

    boolean isFavourite = qLower != null && qLower.contains("favourite");

    for (int i = 0; i < Math.min(3, foodLabels.size()); i++) {
      String label = foodLabels.get(i);
      String iconUrl = i < foods.size() ? foods.get(i).getImageUrl() : null;
      String id = "food-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      String text = isFavourite ? "My favourite food is " + label + "." : DialogueReplyCommon.phraseWouldLike(label);
      replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "FOOD"));
    }
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateSchoolReplies(
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> teachers,
      String qLower,
      String location
  ) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    boolean askWhatDid = qLower != null && (qLower.contains("what did you do") || qLower.contains("what did they do") || qLower.contains("do at school"));

    List<String> subjectLabels = subjects.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    List<String> actLabels = activities.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();

    List<String> schoolActivityLabels = new ArrayList<>(subjectLabels);
    for (String act : actLabels) {
      if (!schoolActivityLabels.contains(act)) {
        schoolActivityLabels.add(act);
      }
    }

    if (askWhatDid && !schoolActivityLabels.isEmpty()) {
      for (int i = 0; i < Math.min(3, schoolActivityLabels.size()); i++) {
        String label = schoolActivityLabels.get(i);
        PreferenceItemEntity entity = DialogueReplyCommon.findPreferenceByLabel(subjects, activities, label);
        String iconUrl = entity != null ? entity.getImageUrl() : null;
        String id = "school-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        String text = "I did " + label + ".";
        replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "ACTIVITY"));
      }
      if (!subjectLabels.isEmpty()) {
        groups.add(new DialogueResponse.OptionGroup("subjects", "Subjects & activities", schoolActivityLabels,
            DialogueReplyCommon.toOptionItemsFromLabels(subjects, activities, schoolActivityLabels)));
      }
    } else {
      replies.add(new DialogueResponse.Reply("school-good", "Good", "It was good.", null, "CONFIRM"));
      replies.add(new DialogueResponse.Reply("school-okay", "Okay", "It was okay.", null, "GENERIC"));
      replies.add(new DialogueResponse.Reply("school-hard", "Not great", "I had a hard day.", null, "REJECT"));
      List<String> teacherLabels = DialogueReplyCommon.labelsOnly(teachers);
      if (!teacherLabels.isEmpty()) {
        groups.add(new DialogueResponse.OptionGroup("teachers", "Teachers", teacherLabels, DialogueReplyCommon.toOptionItems(teachers, teacherLabels)));
      }
      if (!schoolActivityLabels.isEmpty()) {
        groups.add(new DialogueResponse.OptionGroup("subjects", "Subjects & activities", schoolActivityLabels,
            DialogueReplyCommon.toOptionItemsFromLabels(subjects, activities, schoolActivityLabels)));
      }
    }
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateActivityReplies(List<PreferenceItemEntity> activities, String qLower, String location, String favShowFromProfile) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    boolean isTvShowQuestion = qLower != null && (qLower.contains("show") || qLower.contains("tv") || qLower.contains("watch"));
    boolean isGameQuestion = qLower != null && qLower.contains("game");
    boolean isHolidayQuestion = qLower != null && (qLower.contains("holiday") || qLower.contains("place"));

    List<PreferenceItemEntity> filtered = activities;
    if (isTvShowQuestion) {
      List<PreferenceItemEntity> tvShows = activities.stream()
          .filter(e -> e.getCategory() != null && DialogueReplyCommon.isTvShowCategory(e.getCategory()))
          .toList();
      if (!tvShows.isEmpty()) {
        filtered = tvShows;
      } else {
        filtered = List.of();
      }
    } else if (isGameQuestion) {
      List<PreferenceItemEntity> games = activities.stream()
          .filter(e -> e.getCategory() != null && "GAME".equalsIgnoreCase(e.getCategory()))
          .toList();
      filtered = games.isEmpty() ? List.of() : games;
    } else if (isHolidayQuestion) {
      List<PreferenceItemEntity> holiday = activities.stream()
          .filter(e -> e.getCategory() != null && "HOLIDAY".equalsIgnoreCase(e.getCategory()))
          .toList();
      if (!holiday.isEmpty()) filtered = holiday;
    }

    List<String> acts = filtered.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    if (acts.isEmpty()) {
      if (isTvShowQuestion) {
        List<String> tvDefaults = new ArrayList<>();
        if (favShowFromProfile != null && !favShowFromProfile.isBlank()) {
          tvDefaults.add(favShowFromProfile.trim());
        }
        for (String d : List.of("Bluey", "SpongeBob", "Peppa Pig")) {
          if (!tvDefaults.contains(d) && tvDefaults.size() < 3) tvDefaults.add(d);
        }
        acts = tvDefaults.isEmpty() ? List.of("Bluey", "SpongeBob", "Peppa Pig") : tvDefaults;
      } else if (isGameQuestion) {
        acts = List.of("Minecraft", "iPad games", "Drawing");
      } else {
        acts = "SCHOOL".equalsIgnoreCase(location)
            ? List.of("Drawing", "Reading", "Break")
            : List.of("TV", "iPad", "Play", "Outside", "Music", "Drawing");
      }
    }
    String groupTitle = isTvShowQuestion ? "TV Shows" : isGameQuestion ? "Games" : "Activities";
    groups.add(new DialogueResponse.OptionGroup("activities", groupTitle, acts, DialogueReplyCommon.toOptionItems(filtered, acts)));

    boolean isFavourite = qLower != null && (qLower.contains("favourite") || qLower.contains("favorite"));
    String topic = "activity";
    if (qLower != null) {
      if (isTvShowQuestion) topic = "TV show";
      else if (isGameQuestion) topic = "game";
      else if (isHolidayQuestion) topic = "place";
    }

    for (int i = 0; i < Math.min(3, acts.size()); i++) {
      String label = acts.get(i);
      PreferenceItemEntity entity = i < filtered.size() ? filtered.get(i) : null;
      String iconUrl = entity != null ? entity.getImageUrl() : null;
      String id = "act-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      String text = isFavourite ? "My favourite " + topic + " is " + label + "." : DialogueReplyCommon.phraseWouldLike(label);
      replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "ACTIVITY"));
    }
    if (replies.isEmpty()) {
      replies.add(new DialogueResponse.Reply("act-yes", "Yes", "Yes, please.", null, "CONFIRM"));
      replies.add(new DialogueResponse.Reply("act-no", "No", "No, thank you.", null, "REJECT"));
      replies.add(new DialogueResponse.Reply("act-help", "Help", "Can you help me, please?", null, "GENERIC"));
    }
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateGenericPreferenceReplies(
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      String location
  ) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    List<String> drinkLabels = DialogueReplyCommon.labelsOnly(drinks);
    List<String> foodLabels = DialogueReplyCommon.labelsOnly(foods);
    List<String> actLabels = DialogueReplyCommon.labelsOnly(activities);
    if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
    if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
    if (actLabels.isEmpty()) actLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Drawing", "Reading") : List.of("TV", "iPad", "Play");

    if (!drinks.isEmpty()) {
      PreferenceItemEntity d = drinks.get(0);
      replies.add(new DialogueResponse.Reply(
          "drink-" + d.getId(),
          d.getLabel(),
          DialogueReplyCommon.phraseWouldLike(d.getLabel()),
          d.getImageUrl(),
          "DRINK"
      ));
    }
    if (!foods.isEmpty() && replies.size() < 3) {
      PreferenceItemEntity f = foods.get(0);
      replies.add(new DialogueResponse.Reply(
          "food-" + f.getId(),
          f.getLabel(),
          DialogueReplyCommon.phraseWouldLike(f.getLabel()),
          f.getImageUrl(),
          "FOOD"
      ));
    }
    if (!activities.isEmpty() && replies.size() < 3) {
      PreferenceItemEntity a = activities.get(0);
      replies.add(new DialogueResponse.Reply(
          "act-" + a.getId(),
          a.getLabel(),
          DialogueReplyCommon.phraseWouldLike(a.getLabel()),
          a.getImageUrl(),
          "ACTIVITY"
      ));
    }
    while (replies.size() < 3) {
      if (replies.size() == 0 && !drinkLabels.isEmpty()) {
        replies.add(new DialogueResponse.Reply("drink-default", drinkLabels.get(0), DialogueReplyCommon.phraseWouldLike(drinkLabels.get(0)), null, "DRINK"));
      } else if (replies.size() == 1 && !foodLabels.isEmpty()) {
        replies.add(new DialogueResponse.Reply("food-default", foodLabels.get(0), DialogueReplyCommon.phraseWouldLike(foodLabels.get(0)), null, "FOOD"));
      } else if (replies.size() == 2 && !actLabels.isEmpty()) {
        replies.add(new DialogueResponse.Reply("act-default", actLabels.get(0), DialogueReplyCommon.phraseWouldLike(actLabels.get(0)), null, "ACTIVITY"));
      } else break;
    }

    groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
    groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
    groups.add(new DialogueResponse.OptionGroup("activities", "Activities", actLabels));
    return new DialogueReplyEngine.Result(replies, groups);
  }

  static DialogueReplyEngine.Result generateOfferReplies(
      String topic,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      String qLower,
      String location
  ) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    String yesLabel;
    String yesText;
    String noLabel;
    String noText;
    if (qLower != null && qLower.contains("are you hungry")) {
      yesLabel = "Yes";
      yesText = "Yes, I am hungry.";
      noLabel = "No";
      noText = "No, I'm not hungry.";
    } else if (qLower != null && qLower.contains("are you thirsty")) {
      yesLabel = "Yes";
      yesText = "Yes, I am thirsty.";
      noLabel = "No";
      noText = "No, I'm not thirsty.";
    } else if (qLower != null && qLower.contains("are you tired")) {
      yesLabel = "Yes";
      yesText = "Yes, I am tired.";
      noLabel = "No";
      noText = "No, I'm not tired.";
    } else if (qLower != null && qLower.contains("are you excited")) {
      yesLabel = "Yes";
      yesText = "Yes, I am excited.";
      noLabel = "No";
      noText = "No, I'm not excited.";
    } else if (qLower != null && (qLower.contains("are you ready") || qLower.contains("ready to ") || qLower.contains("ready for "))) {
      yesLabel = "Yes";
      yesText = "Yes, I'm ready.";
      noLabel = "No";
      noText = "No, I'm not ready.";
    } else if (qLower != null && qLower.contains("do you have")) {
      yesLabel = "Yes";
      yesText = "Yes, I do.";
      noLabel = "No";
      noText = "No, I don't.";
    } else {
      yesLabel = "Yes please";
      yesText = "Yes, please.";
      noLabel = "No thank you";
      noText = "No, thank you.";
    }
    replies.add(new DialogueResponse.Reply("offer-yes", yesLabel, yesText, null, "CONFIRM"));
    replies.add(new DialogueResponse.Reply("offer-no", noLabel, noText, null, "REJECT"));

    switch (topic) {
      case "food" -> {
        replies.add(new DialogueResponse.Reply("offer-thirsty", "I'm Thirsty", "I'm thirsty. Could I have a drink, please?", null, "DRINK_INSTEAD"));
        List<String> drinkLabels = DialogueReplyCommon.labelsOnly(drinks);
        if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
        List<String> foodLabels = DialogueReplyCommon.labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
        groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
        groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
      }
      case "drink" -> {
        replies.add(new DialogueResponse.Reply("offer-hungry", "I'm Hungry", "I'm hungry. Could I have something to eat, please?", null, "FOOD_INSTEAD"));
        List<String> drinkLabels = DialogueReplyCommon.labelsOnly(drinks);
        if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
        List<String> foodLabels = DialogueReplyCommon.labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
        groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
        groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
      }
      case "activity" -> {
        replies.add(new DialogueResponse.Reply("offer-something-else", "Something Else", "I'd like something else, please.", null, "GENERIC"));
        List<String> actLabels = DialogueReplyCommon.labelsOnly(activities);
        if (actLabels.isEmpty()) actLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Drawing", "Reading", "Break") : List.of("TV", "iPad", "Play", "Outside", "Music", "Drawing");
        groups.add(new DialogueResponse.OptionGroup("activities", "Activities", actLabels));
      }
      default -> {
        if (qLower != null && (qLower.contains("are you ready") || qLower.contains("ready to ") || qLower.contains("ready for "))) {
          replies.add(new DialogueResponse.Reply("offer-not-yet", "Not yet", "Not yet.", null, "REJECT"));
        } else if (qLower != null && qLower.contains("are you tired")) {
          replies.add(new DialogueResponse.Reply("offer-a-bit", "A bit", "I'm a bit tired.", null, "GENERIC"));
        } else if (qLower != null && qLower.contains("are you excited")) {
          replies.add(new DialogueResponse.Reply("offer-a-bit", "A bit", "I'm a bit excited.", null, "GENERIC"));
        } else if (qLower != null && qLower.contains("do you have")) {
          replies.add(new DialogueResponse.Reply("offer-not-sure", "I'm not sure", "I'm not sure.", null, "GENERIC"));
        } else {
          replies.add(new DialogueResponse.Reply("offer-help", "Help", "Can you help me, please?", null, "GENERIC"));
        }
        List<String> drinkLabels = DialogueReplyCommon.labelsOnly(drinks);
        if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
        List<String> foodLabels = DialogueReplyCommon.labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
        groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
        groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
      }
    }

    return new DialogueReplyEngine.Result(replies, groups);
  }
}
