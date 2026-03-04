package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.common.PromptLoader;
import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.service.PreferenceItemService;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DialogueService {

  private static final String SYSTEM_PROMPT =
      PromptLoader.load("prompts/dialogue-system.txt");

  private final AiReplyClient ai;
  private final UserProfileService userProfileService;
  private final ObjectMapper mapper;
  private final PreferenceItemService preferenceItems;
  private final CaregiverProfileService caregiverProfileService;
  private final String dialogueMode;

  public DialogueService(
      AiReplyClient ai,
      UserProfileService userProfileService,
      ObjectMapper mapper,
      PreferenceItemService preferenceItems,
      CaregiverProfileService caregiverProfileService,
      @Value("${dialogue.mode:SEMANTIC}") String dialogueMode
  ) {
    this.ai = ai;
    this.userProfileService = userProfileService;
    this.mapper = mapper;
    this.preferenceItems = preferenceItems;
    this.caregiverProfileService = caregiverProfileService;
    this.dialogueMode = dialogueMode == null ? "SEMANTIC" : dialogueMode.trim().toUpperCase(Locale.ROOT);
  }

  enum Intent {
    DRINK,
    FOOD,
    PETS,
    PET_NAME,
    TEACHER_NAME,
    FAMILY,
    ACTIVITIES,
    SCHOOL,
    FEELING,
    INTRO_REQUEST,  // "What is your name?" -> AAC introduces themselves
    NAME_INTRO,     // "My name is X" -> Nice to meet you options
    OTHER
  }

  public DialogueResponse generateReplies(DialogueRequest req) {
    String q = safe(req.questionText());
    UserProfileService.UserProfile profile = userProfileService.loadProfileOrDefault();

    String qLower = q.toLowerCase(Locale.ROOT);
    Intent intent = detectIntent(qLower);

    boolean llmPrimary = "LLM".equalsIgnoreCase(dialogueMode);
    boolean hybridMode = "HYBRID".equalsIgnoreCase(dialogueMode);
    boolean usedLlm = false;

    List<DialogueResponse.Reply> replies = List.of();
    List<DialogueResponse.OptionGroup> optionGroups = List.of();

    String location = req.context() != null ? req.context().get("location") : null;
    if (location == null || location.isBlank()) location = "HOME";

    List<PreferenceItemEntity> drinks = filterByLocation(preferenceItems.listByKind("DRINK"), location);
    List<PreferenceItemEntity> foods = filterByLocation(preferenceItems.listByKind("FOOD"), location);
    List<PreferenceItemEntity> pets = preferenceItems.listByKind("PET");
    List<PreferenceItemEntity> teachers = preferenceItems.listByKind("TEACHER");
    List<PreferenceItemEntity> family = preferenceItems.listByKind("FAMILY_MEMBER");
    List<PreferenceItemEntity> activities = filterByLocation(preferenceItems.listByKind("ACTIVITY"), location);
    List<PreferenceItemEntity> subjects = preferenceItems.listByKind("SUBJECT");
    String favShow = null;
    try {
      favShow = caregiverProfileService.get().getFavShow();
    } catch (Exception ignored) { /* profile may not exist */ }

    // LLM-primary mode: call the model, then promote from preferences if it returns generic replies.
    if (llmPrimary && ai.isConfigured()) {
      try {
        String userPrompt = buildUserPrompt(
            safe(req.userName()),
            q,
            profile,
            req.memory(),
            intent,
            req.context(),
            foods,
            drinks,
            activities,
            subjects,
            pets,
            teachers,
            family,
            favShow
        );
        String raw = ai.generateJson(SYSTEM_PROMPT, userPrompt);
        String json = extractFirstJsonObject(raw);
        Map<String, Object> root = mapper.readValue(json, new TypeReference<>() {});

        List<Map<String, Object>> top = asListOfMaps(root.get("topReplies"));
        List<Map<String, Object>> groups = asListOfMaps(root.get("optionGroups"));

        List<DialogueResponse.Reply> llmReplies = parseReplies(top);
        List<DialogueResponse.OptionGroup> llmGroups = parseOptionGroups(groups);

        if (!llmReplies.isEmpty()) {
          replies = llmReplies;
          optionGroups = llmGroups;
          usedLlm = true;
        }
      } catch (Exception e) {
        // fall through to semantic handlers below if LLM call fails
      }
    }

    // Semantic handlers as primary (SEMANTIC/HYBRID) or fallback when LLM produced no replies.
    if (replies.isEmpty()) {
      // When the question offers specific options ("juice or water"), use those as tiles.
      Result fromQuestion = generateRepliesFromQuestionOptions(qLower, drinks, foods, activities);
      if (fromQuestion != null && !fromQuestion.replies().isEmpty()) {
        replies = ensureExactly3Replies(fromQuestion.replies());
        optionGroups = fromQuestion.optionGroups();
      } else {
        boolean isOffer = isOfferQuestion(qLower);
        switch (intent) {
        case PET_NAME -> {
          var r = generatePetNameReplies(qLower, pets);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case TEACHER_NAME -> {
          var r = generateTeacherNameReplies(teachers);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case PETS -> {
          var r = generatePetReplies(qLower, pets);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case DRINK -> {
          var r = isOffer ? generateOfferReplies("drink", drinks, foods, List.of(), qLower, location) : generateDrinkReplies(drinks, foods, qLower, location);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case FOOD -> {
          var r = isOffer ? generateOfferReplies("food", drinks, foods, List.of(), qLower, location) : generateFoodReplies(drinks, foods, qLower, location);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case FAMILY -> {
          var r = generateFamilyReplies(family);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case ACTIVITIES -> {
          var r = isOffer ? generateOfferReplies("activity", drinks, foods, activities, qLower, location) : generateActivityReplies(activities, qLower, location, favShow);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case SCHOOL -> {
          var r = generateSchoolReplies(subjects, activities, teachers, qLower, location);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case FEELING -> {
          var r = generateFeelingReplies();
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case INTRO_REQUEST -> {
          var r = generateIntroReplies(req, activities);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        case NAME_INTRO -> {
          var r = generateNameIntroReplies(qLower);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        default -> {
          var r = isOffer ? generateOfferReplies("other", drinks, foods, List.of(), qLower, location) : generateGenericPreferenceReplies(drinks, foods, activities, location);
          replies = r.replies();
          optionGroups = r.optionGroups();
        }
        }
      }
    }

    // HYBRID mode only: semantics first, then LLM override for weak/OTHER replies.
    if (!llmPrimary && hybridMode && ai.isConfigured() && shouldUseLlm(intent, replies)) {
      try {
        String userPrompt = buildUserPrompt(
            safe(req.userName()),
            q,
            profile,
            req.memory(),
            intent,
            req.context(),
            foods,
            drinks,
            activities,
            subjects,
            pets,
            teachers,
            family,
            favShow
        );
        String raw = ai.generateJson(SYSTEM_PROMPT, userPrompt);
        String json = extractFirstJsonObject(raw);
        Map<String, Object> root = mapper.readValue(json, new TypeReference<>() {});

        List<Map<String, Object>> top = asListOfMaps(root.get("topReplies"));
        List<Map<String, Object>> groups = asListOfMaps(root.get("optionGroups"));

        List<DialogueResponse.Reply> llmReplies = parseReplies(top);
        List<DialogueResponse.OptionGroup> llmGroups = parseOptionGroups(groups);

        if (!llmReplies.isEmpty()) {
          replies = llmReplies;
          optionGroups = llmGroups;
          usedLlm = true;
        }
      } catch (Exception e) {
        // Keep semantic replies, but record the error for debugging.
        // debug map is created below.
      }
    }

    if (llmPrimary && usedLlm) {
      // When the question offers specific options ("juice or water"), use those instead of LLM output.
      Result fromQuestion = generateRepliesFromQuestionOptions(qLower, drinks, foods, activities);
      if (fromQuestion != null && !fromQuestion.replies().isEmpty()) {
        replies = ensureExactly3Replies(fromQuestion.replies());
        optionGroups = fromQuestion.optionGroups();
      } else if (isOfferQuestion(qLower) && !areRepliesAppropriateForOffer(replies)) {
        String topic = intent == Intent.FOOD ? "food" : intent == Intent.DRINK ? "drink" : intent == Intent.ACTIVITIES ? "activity" : "other";
        var r = generateOfferReplies(topic, drinks, foods, activities, qLower, location);
        replies = r.replies();
        optionGroups = r.optionGroups();
      } else if (areRepliesGenericByLabel(replies)) {
        // If the LLM returned generic Yes/No/Help (all 3) but we have preference items, promote.
        List<DialogueResponse.OptionGroup> prefGroups = buildPreferenceOptionGroups(
            intent, drinks, foods, activities, subjects, pets, family);
        List<DialogueResponse.OptionGroup> groupsToUse = prefGroups.isEmpty() ? optionGroups : prefGroups;
        if (groupsToUse != null && !groupsToUse.isEmpty()) {
          replies = ensureRepliesFromOptionsIfNeeded(List.of(), groupsToUse);
        }
      }
      replies = ensureExactly3Replies(replies);
    } else {
      // In semantic / hybrid modes, we can safely promote options into tiles when needed.
      replies = ensureRepliesFromOptionsIfNeeded(replies, optionGroups);
      replies = ensureExactly3Replies(replies);
    }

    DialogueResponse.Memory memory = new DialogueResponse.Memory(
        intent.name().toLowerCase(Locale.ROOT),
        q,
        optionGroups
    );

    Map<String, Object> debug = new LinkedHashMap<>();
    debug.put("mode", hybridMode ? "HYBRID" : "PREFERENCES_ONLY");
    debug.put("intent", intent.name());
    debug.put("dialogueMode", dialogueMode);
    debug.put("llmUsed", usedLlm);

    return new DialogueResponse(
        intent.name(),
        replies,
        optionGroups,
        memory,
        debug
    );
  }

  /** Filter preference items by location: HOME/SCHOOL/OUT. Items with BOTH scope appear everywhere. */
  private static List<PreferenceItemEntity> filterByLocation(List<PreferenceItemEntity> items, String location) {
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

  private static Intent detectIntent(String qLower) {
    if (qLower == null || qLower.isBlank()) return Intent.OTHER;
    String q = qLower;
    if (q.contains("drink") || q.contains("thirsty")) return Intent.DRINK;
    if (containsAny(q, "juice", "water", "milk", "milkshake", "smoothie", "hot chocolate", "squash")) {
      return Intent.DRINK;
    }
    if (q.contains("food")
        || q.contains("hungry")
        || q.contains("dinner")
        || q.contains("lunch")
        || q.contains("breakfast")
        || q.contains("snack")
        || q.contains("pizza")
        || q.contains("something to eat")
        || q.contains("want to eat")
        || q.contains("like to eat")
        || q.contains("eat something")) {
      return Intent.FOOD;
    }
    if (containsAny(q, "dog's name", "dogs name", "dog name", "pet's name", "pets name", "pet name", "what is your dog", "whats your dog", "called")) {
      return Intent.PET_NAME;
    }
    if (containsAny(q, "teacher's name", "teachers name", "teacher name", "who is your teacher", "who's your teacher")) {
      return Intent.TEACHER_NAME;
    }
    if (containsAny(q, "dog", "puppy", "cat", "pet", "hamster", "rabbit", "fish", "turtle")) return Intent.PETS;
    if (containsAny(q, "mum", "mom", "mummy", "dad", "daddy", "father", "mother", "sister", "brother", "family", "grandma", "grandpa", "nan", "nanny")) {
      return Intent.FAMILY;
    }
    if (containsAny(q, "play", "game", "ipad", "tablet", "tv", "watch", "outside", "park", "draw", "colour", "color", "music", "song", "dance", "show", "holiday", "place", "favourite place", "place to go")) {
      return Intent.ACTIVITIES;
    }
    if (containsAny(q, "how was school", "how was your day", "how did school go", "how was today")) {
      return Intent.SCHOOL;
    }
    if (q.contains("school")) return Intent.SCHOOL;
    if (containsAny(q, "how are you feeling", "how do you feel", "how are you", "how do you feel today")) {
      return Intent.FEELING;
    }
    if (q.contains("feeling") || q.contains("feel")) return Intent.FEELING;
    if (containsAny(q, "what is your name", "what's your name", "whats your name", "what are you called",
        "tell me your name", "who are you", "your name")) {
      return Intent.INTRO_REQUEST;
    }
    if (containsAny(q, "my name is", "i'm ", "i am ", "call me ") || q.matches(".*\\bi'?m\\s+[a-z]+.*")) {
      return Intent.NAME_INTRO;
    }
    return Intent.OTHER;
  }

  private record Result(List<DialogueResponse.Reply> replies, List<DialogueResponse.OptionGroup> optionGroups) {}

  private static Result generatePetNameReplies(String qLower, List<PreferenceItemEntity> pets) {
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
    groups.add(new DialogueResponse.OptionGroup("pets", "Pets", labelsOnly(pets)));
    return new Result(replies, groups);
  }

  private static Result generateTeacherNameReplies(List<PreferenceItemEntity> teachers) {
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
    groups.add(new DialogueResponse.OptionGroup("teachers", "Teachers", labelsOnly(teachers)));
    return new Result(replies, groups);
  }

  private static Result generatePetReplies(String qLower, List<PreferenceItemEntity> pets) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    String asked = petTypeFromQuestion(qLower);
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
    return new Result(replies, groups);
  }

  private static Result generateDrinkReplies(List<PreferenceItemEntity> drinks, List<PreferenceItemEntity> foods, String qLower, String location) {
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

    groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels, toOptionItems(drinks, drinkLabels)));
    groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels, toOptionItems(foods, foodLabels)));

    boolean isFavourite = qLower != null && qLower.contains("favourite");

    for (int i = 0; i < Math.min(3, drinkLabels.size()); i++) {
      String label = drinkLabels.get(i);
      String iconUrl = i < drinks.size() ? drinks.get(i).getImageUrl() : null;
      String id = "drink-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      String text = isFavourite ? "My favourite drink is " + label + "." : phraseWouldLike(label);
      replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "DRINK"));
    }
    return new Result(replies, groups);
  }

  private static Result generateFoodReplies(List<PreferenceItemEntity> drinks, List<PreferenceItemEntity> foods, String qLower, String location) {
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

    groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels, toOptionItems(foods, foodLabels)));
    groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels, toOptionItems(drinks, drinkLabels)));

    boolean isFavourite = qLower != null && qLower.contains("favourite");

    for (int i = 0; i < Math.min(3, foodLabels.size()); i++) {
      String label = foodLabels.get(i);
      String iconUrl = i < foods.size() ? foods.get(i).getImageUrl() : null;
      String id = "food-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      String text = isFavourite ? "My favourite food is " + label + "." : phraseWouldLike(label);
      replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "FOOD"));
    }
    return new Result(replies, groups);
  }

  private static Result generateFamilyReplies(List<PreferenceItemEntity> family) {
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
    return new Result(replies, groups);
  }

  private static Result generateSchoolReplies(
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> teachers,
      String qLower,
      String location
  ) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    // "What did you do at school?" → offer subjects and school activities; "How was school?" → offer moods
    boolean askWhatDid = qLower != null && (qLower.contains("what did you do") || qLower.contains("what did they do") || qLower.contains("do at school"));

    List<String> subjectLabels = subjects.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();
    List<String> actLabels = activities.stream()
        .map(PreferenceItemEntity::getLabel)
        .filter(s -> s != null && !s.isBlank())
        .toList();

    // For "what did you do", use subjects first (from School settings), then activities (scope SCHOOL/BOTH)
    List<String> schoolActivityLabels = new ArrayList<>(subjectLabels);
    for (String act : actLabels) {
      if (!schoolActivityLabels.contains(act)) {
        schoolActivityLabels.add(act);
      }
    }

    if (askWhatDid && !schoolActivityLabels.isEmpty()) {
      for (int i = 0; i < Math.min(3, schoolActivityLabels.size()); i++) {
        String label = schoolActivityLabels.get(i);
        PreferenceItemEntity entity = findPreferenceByLabel(subjects, activities, label);
        String iconUrl = entity != null ? entity.getImageUrl() : null;
        String id = "school-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        String text = "I did " + label + ".";
        replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "ACTIVITY"));
      }
      if (!subjectLabels.isEmpty()) {
        groups.add(new DialogueResponse.OptionGroup("subjects", "Subjects & activities", schoolActivityLabels, toOptionItemsFromLabels(subjects, activities, schoolActivityLabels)));
      }
    } else {
      replies.add(new DialogueResponse.Reply("school-good", "Good", "It was good.", null, "CONFIRM"));
      replies.add(new DialogueResponse.Reply("school-okay", "Okay", "It was okay.", null, "GENERIC"));
      replies.add(new DialogueResponse.Reply("school-hard", "Not great", "I had a hard day.", null, "REJECT"));
      List<String> teacherLabels = labelsOnly(teachers);
      if (!teacherLabels.isEmpty()) {
        groups.add(new DialogueResponse.OptionGroup("teachers", "Teachers", teacherLabels, toOptionItems(teachers, teacherLabels)));
      }
      if (!schoolActivityLabels.isEmpty()) {
        groups.add(new DialogueResponse.OptionGroup("subjects", "Subjects & activities", schoolActivityLabels, toOptionItemsFromLabels(subjects, activities, schoolActivityLabels)));
      }
    }
    return new Result(replies, groups);
  }

  private static PreferenceItemEntity findPreferenceByLabel(
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> activities,
      String label
  ) {
    if (label == null) return null;
    return subjects.stream().filter(e -> label.equals(e.getLabel())).findFirst()
        .or(() -> activities.stream().filter(e -> label.equals(e.getLabel())).findFirst())
        .orElse(null);
  }

  private static List<DialogueResponse.OptionItem> toOptionItemsFromLabels(
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

  private static Result generateFeelingReplies() {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    replies.add(new DialogueResponse.Reply("feel-good", "Good", "I feel good.", null, "CONFIRM"));
    replies.add(new DialogueResponse.Reply("feel-okay", "Okay", "I'm okay.", null, "GENERIC"));
    replies.add(new DialogueResponse.Reply("feel-sad", "Not great", "I don't feel great.", null, "REJECT"));
    return new Result(replies, List.of());
  }

  private Result generateIntroReplies(DialogueRequest req, List<PreferenceItemEntity> activities) {
    String name = safe(req.userName());
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
    } catch (Exception ignored) {
      // Fall back to minimal intro
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

  private static Result generateNameIntroReplies(String qLower) {
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
    return new Result(replies, List.of());
  }

  private static String extractNameFromIntro(String q) {
    if (q == null || q.isBlank()) return null;
    String s = q.trim();
    String[] patterns = { "my name is ", "i'm ", "i am ", "call me ", "im " };
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

  private static Result generateActivityReplies(List<PreferenceItemEntity> activities, String qLower, String location, String favShowFromProfile) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    boolean isTvShowQuestion = qLower != null && (qLower.contains("show") || qLower.contains("tv") || qLower.contains("watch"));
    boolean isGameQuestion = qLower != null && qLower.contains("game");
    boolean isHolidayQuestion = qLower != null && (qLower.contains("holiday") || qLower.contains("place"));

    // Filter activities by question type so "favourite TV show" gets TV shows, not games/activities
    List<PreferenceItemEntity> filtered = activities;
    if (isTvShowQuestion) {
      List<PreferenceItemEntity> tvShows = activities.stream()
          .filter(e -> e.getCategory() != null && isTvShowCategory(e.getCategory()))
          .toList();
      if (!tvShows.isEmpty()) {
        filtered = tvShows;
      } else {
        // No TV_SHOW-categorized items: use defaults so answers stay on-topic
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
        // Use carer's fav_show from user_profile (V7 seed) when no TV_SHOW preference items
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
    groups.add(new DialogueResponse.OptionGroup("activities", groupTitle, acts, toOptionItems(filtered, acts)));

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
      String text = isFavourite ? "My favourite " + topic + " is " + label + "." : phraseWouldLike(label);
      replies.add(new DialogueResponse.Reply(id, label, text, iconUrl, "ACTIVITY"));
    }
    // Fallback only when no activities at all
    if (replies.isEmpty()) {
      replies.add(new DialogueResponse.Reply("act-yes", "Yes", "Yes, please.", null, "CONFIRM"));
      replies.add(new DialogueResponse.Reply("act-no", "No", "No, thank you.", null, "REJECT"));
      replies.add(new DialogueResponse.Reply("act-help", "Help", "Can you help me, please?", null, "GENERIC"));
    }
    return new Result(replies, groups);
  }

  /**
   * For abstract/OTHER questions: return 3 concrete items from the user's preferences
   * (drink, food, activity) so the LLM-style "guess" is personalized, not generic Drink/Food/Help.
   */
  private static Result generateGenericPreferenceReplies(
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      String location
  ) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    List<String> drinkLabels = labelsOnly(drinks);
    List<String> foodLabels = labelsOnly(foods);
    List<String> actLabels = labelsOnly(activities);
    if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
    if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
    if (actLabels.isEmpty()) actLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Drawing", "Reading") : List.of("TV", "iPad", "Play");

    if (!drinks.isEmpty()) {
      PreferenceItemEntity d = drinks.get(0);
      replies.add(new DialogueResponse.Reply(
          "drink-" + d.getId(),
          d.getLabel(),
          phraseWouldLike(d.getLabel()),
          d.getImageUrl(),
          "DRINK"
      ));
    }
    if (!foods.isEmpty() && replies.size() < 3) {
      PreferenceItemEntity f = foods.get(0);
      replies.add(new DialogueResponse.Reply(
          "food-" + f.getId(),
          f.getLabel(),
          phraseWouldLike(f.getLabel()),
          f.getImageUrl(),
          "FOOD"
      ));
    }
    if (!activities.isEmpty() && replies.size() < 3) {
      PreferenceItemEntity a = activities.get(0);
      replies.add(new DialogueResponse.Reply(
          "act-" + a.getId(),
          a.getLabel(),
          phraseWouldLike(a.getLabel()),
          a.getImageUrl(),
          "ACTIVITY"
      ));
    }
    while (replies.size() < 3) {
      if (replies.size() == 0 && !drinkLabels.isEmpty()) {
        replies.add(new DialogueResponse.Reply("drink-default", drinkLabels.get(0), phraseWouldLike(drinkLabels.get(0)), null, "DRINK"));
      } else if (replies.size() == 1 && !foodLabels.isEmpty()) {
        replies.add(new DialogueResponse.Reply("food-default", foodLabels.get(0), phraseWouldLike(foodLabels.get(0)), null, "FOOD"));
      } else if (replies.size() == 2 && !actLabels.isEmpty()) {
        replies.add(new DialogueResponse.Reply("act-default", actLabels.get(0), phraseWouldLike(actLabels.get(0)), null, "ACTIVITY"));
      } else break;
    }

    groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
    groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
    groups.add(new DialogueResponse.OptionGroup("activities", "Activities", actLabels));
    return new Result(replies, groups);
  }

  /**
   * True only when ALL replies are generic (Yes/No/Help/Again). Do NOT promote when the LLM
   * returned a meaningful third option (e.g. "I'm Thirsty", "Drink Instead") for yes/no questions.
   */
  private static boolean areRepliesGenericByLabel(List<DialogueResponse.Reply> replies) {
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
    // Only promote when ALL 3 are generic (e.g. Yes, No, Help). If any is meaningful (I'm Thirsty),
    // keep the LLM's response.
    return genericCount >= 3;
  }

  /** Build option groups from preference data for the given intent. */
  private static List<DialogueResponse.OptionGroup> buildPreferenceOptionGroups(
      Intent intent,
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
        List<String> subLabels = labelsOnly(subjects);
        List<String> actLabels = labelsOnly(activities);
        labels = new ArrayList<>(subLabels);
        for (String a : actLabels) {
          if (!labels.contains(a)) labels.add(a);
        }
        if (labels.isEmpty()) labels = List.of("Drawing", "Reading", "Maths");
        out.add(new DialogueResponse.OptionGroup("subjects", "Subjects & activities", labels, toOptionItemsFromLabels(subjects, activities, labels)));
      }
      case DRINK -> {
        labels = labelsOnly(drinks);
        if (labels.isEmpty()) labels = List.of("Water", "Juice", "Milk");
        out.add(new DialogueResponse.OptionGroup("drinks", "Drinks", labels, toOptionItems(drinks, labels)));
      }
      case FOOD -> {
        labels = labelsOnly(foods);
        if (labels.isEmpty()) labels = List.of("Toast", "Banana", "Sandwich");
        out.add(new DialogueResponse.OptionGroup("foods", "Foods", labels, toOptionItems(foods, labels)));
      }
      case ACTIVITIES -> {
        labels = labelsOnly(activities);
        if (labels.isEmpty()) labels = List.of("TV", "iPad", "Play", "Outside", "Music", "Drawing");
        out.add(new DialogueResponse.OptionGroup("activities", "Activities", labels, toOptionItems(activities, labels)));
      }
      case PETS -> {
        labels = labelsOnly(pets);
        if (labels.isEmpty()) labels = List.of("Dog", "Cat", "Fish", "Rabbit");
        out.add(new DialogueResponse.OptionGroup("pets", "Pets", labels, toOptionItems(pets, labels)));
      }
      case FAMILY -> {
        labels = labelsOnly(family);
        if (labels.isEmpty()) labels = List.of("Mum", "Dad", "Brother", "Sister", "Nan", "Grandad");
        out.add(new DialogueResponse.OptionGroup("family", "People", labels, toOptionItems(family, labels)));
      }
      default -> {
        labels = labelsOnly(drinks);
        if (labels.isEmpty()) labels = List.of("Water", "Juice", "Milk");
        List<String> foodLabels = labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = List.of("Toast", "Banana", "Sandwich");
        out.add(new DialogueResponse.OptionGroup("drinks", "Drinks", labels, toOptionItems(drinks, labels)));
        out.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels, toOptionItems(foods, foodLabels)));
      }
    }
    return out;
  }

  private static boolean shouldUseLlm(Intent intent, List<DialogueResponse.Reply> replies) {
    if (intent == null) return true;
    if (replies == null || replies.isEmpty()) return true;
    if (intent != Intent.OTHER) return false;

    boolean allGeneric = true;
    for (DialogueResponse.Reply r : replies) {
      String kind = r.kind();
      if (kind != null && !kind.equalsIgnoreCase("GENERIC")) {
        allGeneric = false;
        break;
      }
    }
    return allGeneric;
  }

  /** True if replies look appropriate for an offer question (Yes/No + alternative like I'm Thirsty). */
  private static boolean areRepliesAppropriateForOffer(List<DialogueResponse.Reply> replies) {
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
    return match >= 2; // at least Yes+No or Yes+alternative
  }

  /**
   * True if the question is an offer (yes/no) needing Yes/No + alternative.
   * "What would you like to drink?" is a CHOICE (show drinks), not an offer.
   * "Would you like a drink?" is an offer (yes/no).
   */
  private static boolean isOfferQuestion(String qLower) {
    if (qLower == null || qLower.isBlank()) return false;
    if (qLower.contains("what would you like") || qLower.contains("what do you want")
        || qLower.contains("which would you like") || qLower.contains("which do you want")) {
      return false; // choice question - show options
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

  /**
   * When the question offers specific options (e.g. "juice or water", "big glass or small glass of juice"),
   * extract the full option phrase from each part after " or " and use them as tiles.
   */
  private static Result generateRepliesFromQuestionOptions(
      String qLower,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities
  ) {
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
      String displayLabel = displayLabelFor(opt);
      String id = "opt-" + opt.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
      replies.add(new DialogueResponse.Reply(id, displayLabel, phraseWouldLike(opt), null, null));
      if (replies.size() >= 3) break;
    }
    List<String> displayLabels = rawOpts.stream().map(DialogueService::displayLabelFor).toList();
    groups.add(new DialogueResponse.OptionGroup("choices", "Your choices", displayLabels));
    return new Result(replies, groups);
  }

  private static String toTitleCase(String s) {
    if (s == null || s.isBlank()) return s;
    return Arrays.stream(s.split("\\s+"))
        .map(w -> w.length() > 1 ? w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1).toLowerCase(Locale.ROOT) : w)
        .collect(Collectors.joining(" "));
  }

  /** For display labels: strip "your " so tiles show "iPhone" not "Your iPhone". */
  private static String displayLabelFor(String rawOption) {
    if (rawOption == null || rawOption.isBlank()) return rawOption;
    String lower = rawOption.toLowerCase(Locale.ROOT);
    if (lower.startsWith("your ")) {
      return toTitleCase(rawOption.substring(5).trim());
    }
    return toTitleCase(rawOption);
  }

  /**
   * Prefix label with "a" or "an" for countable nouns in "I would like X please".
   * Skips article for uncountable (water, juice, milk), quantities (one, two, 2), and proper nouns (Paw Patrol, Bluey).
   */
  private static String withArticle(String label) {
    if (label == null || label.isBlank()) return label;
    String trimmed = label.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    // Quantity prefix – no article ("two cupcakes", "one apple", "2 biscuits")
    if (lower.matches("^(one|two|three|four|five|six|seven|eight|nine|ten)\\s+.+")
        || lower.matches("^\\d+\\s+.+")) {
      return trimmed;
    }
    // Uncountable – no article
    if (lower.contains("juice") || lower.contains("milk") || lower.contains("water") || lower.contains("tea")
        || lower.contains("coffee") || lower.contains("squash") || lower.contains("toast") || lower.contains("bread")
        || lower.contains("pasta") || lower.contains("rice") || lower.contains("cereal")) {
      return trimmed;
    }
    // Multi-word proper noun (e.g. "Paw Patrol", "Peppa Pig") – but NOT food compounds like "ham sandwich"
    if (trimmed.contains(" ")) {
      int spaceIdx = trimmed.indexOf(' ');
      if (spaceIdx + 1 < trimmed.length() && Character.isUpperCase(trimmed.charAt(spaceIdx + 1))) {
        // Food compound words need article: ham sandwich, cheese toastie, chicken wrap, etc.
        if (containsAny(lower, "sandwich", "burger", "wrap", "salad", "pizza", "toastie", "roll", "bagel", "muffin", "cookie", "biscuit")) {
          // fall through to article logic below
        } else {
          return trimmed;
        }
      }
    }
    // Vowel sound → "an" (apple, orange, egg, etc.)
    if (lower.startsWith("a") || lower.startsWith("e") || lower.startsWith("i") || lower.startsWith("o")
        || (lower.startsWith("u") && !lower.startsWith("uk"))) {
      return "an " + trimmed;
    }
    return "a " + trimmed;
  }

  private static String phraseWouldLike(String label) {
    if (label == null || label.isBlank()) return "I would like it, please.";
    String trimmed = label.trim();
    // Caregiver asks "your X" (child's possession); child speaks "my X"
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("your ")) {
      String rest = trimmed.substring(5).trim();
      if (rest.isEmpty()) return "I would like it, please.";
      return "I would like my " + rest + ", please.";
    }
    return "I would like " + withArticle(label) + ", please.";
  }

  /** Yes/No + one alternative for offer questions. For "Are you hungry?" use "Yes, I am hungry" not "Yes, please". */
  private static Result generateOfferReplies(
      String topic,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> activities,
      String qLower,
      String location
  ) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();
    List<DialogueResponse.OptionGroup> groups = new ArrayList<>();

    String yesLabel, yesText, noLabel, noText;
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
        List<String> drinkLabels = labelsOnly(drinks);
        if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
        List<String> foodLabels = labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
        groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
        groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
      }
      case "drink" -> {
        replies.add(new DialogueResponse.Reply("offer-hungry", "I'm Hungry", "I'm hungry. Could I have something to eat, please?", null, "FOOD_INSTEAD"));
        List<String> drinkLabels = labelsOnly(drinks);
        if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
        List<String> foodLabels = labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
        groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
        groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
      }
      case "activity" -> {
        replies.add(new DialogueResponse.Reply("offer-something-else", "Something Else", "I'd like something else, please.", null, "GENERIC"));
        List<String> actLabels = labelsOnly(activities);
        if (actLabels.isEmpty()) actLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Drawing", "Reading", "Break") : List.of("TV", "iPad", "Play", "Outside", "Music", "Drawing");
        groups.add(new DialogueResponse.OptionGroup("activities", "Activities", actLabels));
      }
      default -> {
        // Contextual third option for yes/no questions
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
        List<String> drinkLabels = labelsOnly(drinks);
        if (drinkLabels.isEmpty()) drinkLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Water") : List.of("Water", "Juice", "Milk");
        List<String> foodLabels = labelsOnly(foods);
        if (foodLabels.isEmpty()) foodLabels = "SCHOOL".equalsIgnoreCase(location) ? List.of("Sandwich", "Fruit") : List.of("Toast", "Banana", "Sandwich");
        groups.add(new DialogueResponse.OptionGroup("drinks", "Drinks", drinkLabels));
        groups.add(new DialogueResponse.OptionGroup("foods", "Foods", foodLabels));
      }
    }

    return new Result(replies, groups);
  }

  private static boolean containsAny(String haystack, String... needles) {
    if (haystack == null || haystack.isBlank()) return false;
    for (String n : needles) {
      if (n != null && !n.isBlank() && haystack.contains(n)) return true;
    }
    return false;
  }

  private static boolean isTvShowCategory(String category) {
    if (category == null) return false;
    String c = category.toUpperCase(Locale.ROOT);
    return "TV_SHOW".equals(c) || "TV".equals(c) || "SHOW".equals(c);
  }

  private static String formatActivitiesByCategory(List<PreferenceItemEntity> activities) {
    if (activities == null || activities.isEmpty()) return "";
    Map<String, List<String>> byCat = new LinkedHashMap<>();
    for (PreferenceItemEntity e : activities) {
      String label = e.getLabel();
      if (label == null || label.isBlank()) continue;
      String cat = e.getCategory() != null ? e.getCategory().toUpperCase(Locale.ROOT) : "OTHER";
      byCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(label.trim());
    }
    String catStr = byCat.entrySet().stream()
        .filter(entry -> !"OTHER".equals(entry.getKey()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .reduce((a, b) -> a + ", " + b)
        .orElse(null);
    if (catStr == null || catStr.isBlank()) return "";
    return "  activitiesByCategory: " + catStr + "\n";
  }

  private static String petTypeFromQuestion(String qLower) {
    if (qLower == null) return "PET";
    if (qLower.contains("dog") || qLower.contains("puppy")) return "DOG";
    if (qLower.contains("cat")) return "CAT";
    return "PET";
  }

  private static List<DialogueResponse.OptionItem> toOptionItems(List<PreferenceItemEntity> entities, List<String> labels) {
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

  private static List<String> labelsOnly(List<PreferenceItemEntity> entities) {
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

  /**
   * If the model put good suggestions only into optionGroups, promote some of those items
   * into topReplies so they appear as the main tiles.
   */
  private static List<DialogueResponse.Reply> ensureRepliesFromOptionsIfNeeded(
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
        String text = phraseWouldLike(label);
        String id = "opt-" + label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        out.add(new DialogueResponse.Reply(id, label, text, null, null));
      }
      if (out.size() >= 3) break;
    }

    return out;
  }

  private static String buildUserPrompt(
      String userName,
      String question,
      UserProfileService.UserProfile profile,
      DialogueRequest.ConversationMemory mem,
      Intent detectedIntent,
      Map<String, String> context,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> pets,
      List<PreferenceItemEntity> teachers,
      List<PreferenceItemEntity> family,
      String favShowFromCarer
  ) {
    StringBuilder sb = new StringBuilder();

    sb.append("userName: ").append(userName).append("\n");
    sb.append("questionText: ").append(question).append("\n");
    sb.append("detectedIntentHint: ").append(detectedIntent == null ? "OTHER" : detectedIntent.name()).append("\n");

    if (context != null && !context.isEmpty()) {
      sb.append("context:\n");
      context.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
    } else {
      sb.append("context: none\n");
    }
    sb.append("\n");

    // Keep these as "style preferences" only, not as a required structure.
    sb.append("stylePreferences:\n");
    sb.append("- yesPrefix: ").append(profile.yesPrefix()).append("\n");
    sb.append("- noText: ").append(profile.noText()).append("\n");
    sb.append("- repeatText: ").append(profile.repeatText()).append("\n\n");

    sb.append("profilePreferences:\n");
    sb.append("- likedFoods: ").append(profile.likedFoods()).append("\n");
    sb.append("- dislikedFoods: ").append(profile.dislikedFoods()).append("\n");
    sb.append("- likedDrinks: ").append(profile.likedDrinks()).append("\n");
    sb.append("- dislikedDrinks: ").append(profile.dislikedDrinks()).append("\n\n");

    sb.append("caregiverPreferenceItems:\n");
    sb.append("  foods: ").append(labelsOnly(foods)).append("\n");
    sb.append("  drinks: ").append(labelsOnly(drinks)).append("\n");
    sb.append("  activities: ").append(labelsOnly(activities)).append("\n");
    sb.append("  subjects: ").append(labelsOnly(subjects)).append(" (subjects & activities from School settings – use for 'What did you do at school today?')\n");
    String byCat = formatActivitiesByCategory(activities);
    if (!byCat.isEmpty()) sb.append(byCat);
    if (favShowFromCarer != null && !favShowFromCarer.isBlank()) {
      sb.append("  carerFavShow: ").append(favShowFromCarer.trim()).append(" (use for 'favourite TV show' when activities lack TV_SHOW)\n");
    }
    sb.append("  pets: ").append(labelsOnly(pets)).append("\n");
    sb.append("  teachers: ").append(labelsOnly(teachers)).append("\n");
    sb.append("  family: ").append(labelsOnly(family)).append("\n\n");

    if (mem != null) {
      sb.append("conversationMemory:\n");
      sb.append("- lastIntent: ").append(mem.lastIntent()).append("\n");
      sb.append("- lastQuestionText: ").append(mem.lastQuestionText()).append("\n");
      sb.append("- lastOptionGroups: ").append(mem.lastOptionGroups()).append("\n");
    } else {
      sb.append("conversationMemory: none\n");
    }

    sb.append("\nGUIDANCE:\n");
    sb.append("- Use caregiverPreferenceItems as the PRIMARY source for concrete options.\n");
    sb.append("- For 'What is your favourite X?' questions, the reply text MUST be 'My favourite [topic] is [item].' (e.g. 'My favourite TV show is Bluey.'). Do NOT use 'I would like X, please.' for favourite questions.\n");
    sb.append("- For 'What is the dog's/teacher's name?' use teachers and pets lists. Reply with full sentences: 'My dog's name is Bella.' or 'My teacher's name is Ms Smith.'\n");
    sb.append("- When the intent is drink_offer or food_offer, replies and optionGroups.items should usually reference these lists.\n");
    sb.append("- Only invent new options when there is no relevant preference data.\n");

    return sb.toString();
  }

  private static List<DialogueResponse.Reply> parseReplies(List<Map<String, Object>> top) {
    List<DialogueResponse.Reply> replies = new ArrayList<>();

    for (Map<String, Object> r : top) {
      String id = asString(r.get("id"), UUID.randomUUID().toString());
      String label = asString(r.get("label"), "Option");
      String text = asString(r.get("text"), "");

      if (!text.isBlank()) {
        replies.add(new DialogueResponse.Reply(id, label, text, null, null));
      }
    }

    return replies;
  }

  private static List<DialogueResponse.Reply> ensureExactly3Replies(List<DialogueResponse.Reply> in) {
    List<DialogueResponse.Reply> out = new ArrayList<>();
    if (in != null) out.addAll(in);

    // Truncate if more than 3
    if (out.size() > 3) {
      return out.subList(0, 3);
    }

    // Pad if less than 3 (do NOT force yes/no/instead)
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

  private static List<DialogueResponse.OptionGroup> parseOptionGroups(List<Map<String, Object>> groups) {
    List<DialogueResponse.OptionGroup> optionGroups = new ArrayList<>();

    for (Map<String, Object> g : groups) {
      optionGroups.add(new DialogueResponse.OptionGroup(
          asString(g.get("id"), "options"),
          asString(g.get("title"), "More options"),
          asStringList(g.get("items"))
      ));
    }

    return optionGroups;
  }

  private DialogueResponse fallback(String reason, String error) {
    var replies = List.of(
        new DialogueResponse.Reply("r1", "Again", "Can you say it again, please?", null, "GENERIC"),
        new DialogueResponse.Reply("r2", "Show me", "Can you show me, please?", null, "GENERIC"),
        new DialogueResponse.Reply("r3", "Help", "Can you help me, please?", null, "GENERIC")
    );

    Map<String, Object> debug = new LinkedHashMap<>();
    debug.put("mode", "FALLBACK");
    debug.put("reason", reason);
    if (error != null && !error.isBlank()) debug.put("error", error);

    DialogueResponse.Memory memory = new DialogueResponse.Memory("other", "", List.of());

    return new DialogueResponse(
        "other",
        replies,
        List.of(),
        memory,
        debug
    );
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private static String asString(Object value, String fallback) {
    String s = value == null ? "" : value.toString().trim();
    return s.isBlank() ? fallback : s;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> asListOfMaps(Object value) {
    if (value instanceof List<?> list) {
      List<Map<String, Object>> out = new ArrayList<>();
      for (Object o : list) {
        if (o instanceof Map<?, ?> m) {
          Map<String, Object> converted = new LinkedHashMap<>();
          for (var e : m.entrySet()) {
            if (e.getKey() != null) converted.put(e.getKey().toString(), e.getValue());
          }
          out.add(converted);
        }
      }
      return out;
    }
    return List.of();
  }

  private static List<String> asStringList(Object value) {
    if (value instanceof List<?> list) {
      List<String> out = new ArrayList<>();
      for (Object o : list) {
        if (o != null) {
          String s = o.toString().trim();
          if (!s.isBlank()) out.add(s);
        }
      }
      return out;
    }
    return List.of();
  }

  private static String extractFirstJsonObject(String s) {
    if (s == null) return "";
    int start = s.indexOf('{');
    int end = s.lastIndexOf('}');
    if (start >= 0 && end > start) return s.substring(start, end + 1);
    return s.trim();
  }

  private static String safeMessage(Exception e) {
    String msg = e.getMessage();
    if (msg == null) return e.getClass().getSimpleName();
    return msg.length() > 180 ? msg.substring(0, 180) : msg;
  }
}