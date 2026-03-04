package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.common.PromptLoader;
import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.service.PreferenceItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DialogueService {

  private static final Logger log = LoggerFactory.getLogger(DialogueService.class);
  private static final String SYSTEM_PROMPT = PromptLoader.load("prompts/dialogue-system.txt");

  private final AiReplyClient ai;
  private final UserProfileService userProfileService;
  private final ObjectMapper mapper;
  private final PreferenceItemService preferenceItems;
  private final DialogueReplyEngine replyEngine;
  private final DialogueSemanticEngine semanticEngine;
  private final DialogueMode dialogueMode;

  public DialogueService(
      AiReplyClient ai,
      UserProfileService userProfileService,
      ObjectMapper mapper,
      PreferenceItemService preferenceItems,
      DialogueReplyEngine replyEngine,
      DialogueSemanticEngine semanticEngine,
      @Value("${dialogue.mode:SEMANTIC}") String dialogueMode
  ) {
    this.ai = ai;
    this.userProfileService = userProfileService;
    this.mapper = mapper;
    this.preferenceItems = preferenceItems;
    this.replyEngine = replyEngine;
    this.semanticEngine = semanticEngine;
    this.dialogueMode = DialogueMode.from(dialogueMode);
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
    INTRO_REQUEST,
    NAME_INTRO,
    OTHER
  }

  public DialogueResponse generateReplies(DialogueRequest req) {
    String q = safe(req.questionText());
    UserProfileService.UserProfile profile = userProfileService.loadProfileOrDefault();

    String qLower = q.toLowerCase(Locale.ROOT);
    Intent intent = detectIntent(qLower);

    boolean llmPrimary = dialogueMode == DialogueMode.LLM;
    boolean hybridMode = dialogueMode == DialogueMode.HYBRID;
    boolean usedLlm = false;

    List<DialogueResponse.Reply> replies = List.of();
    List<DialogueResponse.OptionGroup> optionGroups = List.of();

    String location = req.context() != null ? req.context().get("location") : null;
    if (location == null || location.isBlank()) location = "HOME";

    List<PreferenceItemEntity> drinks = DialogueReplyEngine.filterByLocation(preferenceItems.listByKind(PreferenceKind.DRINK.value()), location);
    List<PreferenceItemEntity> foods = DialogueReplyEngine.filterByLocation(preferenceItems.listByKind(PreferenceKind.FOOD.value()), location);
    List<PreferenceItemEntity> pets = preferenceItems.listByKind(PreferenceKind.PET.value());
    List<PreferenceItemEntity> teachers = preferenceItems.listByKind(PreferenceKind.TEACHER.value());
    List<PreferenceItemEntity> family = preferenceItems.listByKind(PreferenceKind.FAMILY_MEMBER.value());
    List<PreferenceItemEntity> activities = DialogueReplyEngine.filterByLocation(preferenceItems.listByKind(PreferenceKind.ACTIVITY.value()), location);
    List<PreferenceItemEntity> subjects = preferenceItems.listByKind(PreferenceKind.SUBJECT.value());
    String favShow = replyEngine.getFavShowOrNull();

    if (llmPrimary && ai.isConfigured()) {
      try {
        LlmResult llmResult = generateWithLlm(req, profile, intent, q, foods, drinks, activities, subjects, pets, teachers, family, favShow);
        if (llmResult != null && !llmResult.replies().isEmpty()) {
          replies = llmResult.replies();
          optionGroups = llmResult.optionGroups();
          usedLlm = true;
        }
      } catch (Exception ex) {
        log.warn("LLM primary mode failed; falling back to semantic. intent={}, question='{}'", intent, q, ex);
      }
    }

    if (replies.isEmpty()) {
      DialogueReplyEngine.Result semantic = semanticEngine.generate(
          intent, qLower, location, req, drinks, foods, activities, subjects, pets, teachers, family, favShow
      );
      replies = semantic.replies();
      optionGroups = semantic.optionGroups();
    }

    if (!llmPrimary && hybridMode && ai.isConfigured() && shouldUseLlm(intent, replies)) {
      try {
        LlmResult llmResult = generateWithLlm(req, profile, intent, q, foods, drinks, activities, subjects, pets, teachers, family, favShow);
        if (llmResult != null && !llmResult.replies().isEmpty()) {
          replies = llmResult.replies();
          optionGroups = llmResult.optionGroups();
          usedLlm = true;
        }
      } catch (Exception ex) {
        log.warn("LLM hybrid override failed; keeping semantic replies. intent={}, question='{}'", intent, q, ex);
      }
    }

    if (llmPrimary && usedLlm) {
      DialogueReplyEngine.Result fromQuestion = DialogueReplyEngine.generateRepliesFromQuestionOptions(qLower, drinks, foods, activities);
      if (fromQuestion != null && !fromQuestion.replies().isEmpty()) {
        replies = DialogueReplyEngine.ensureExactly3Replies(fromQuestion.replies());
        optionGroups = fromQuestion.optionGroups();
      } else if (DialogueReplyEngine.isOfferQuestion(qLower) && !DialogueReplyEngine.areRepliesAppropriateForOffer(replies)) {
        String topic = intent == Intent.FOOD ? "food" : intent == Intent.DRINK ? "drink" : intent == Intent.ACTIVITIES ? "activity" : "other";
        var r = DialogueReplyEngine.generateOfferReplies(topic, drinks, foods, activities, qLower, location);
        replies = r.replies();
        optionGroups = r.optionGroups();
      } else if (DialogueReplyEngine.areRepliesGenericByLabel(replies)) {
        List<DialogueResponse.OptionGroup> prefGroups = DialogueReplyEngine.buildPreferenceOptionGroups(
            intent, drinks, foods, activities, subjects, pets, family
        );
        List<DialogueResponse.OptionGroup> groupsToUse = prefGroups.isEmpty() ? optionGroups : prefGroups;
        if (groupsToUse != null && !groupsToUse.isEmpty()) {
          replies = DialogueReplyEngine.ensureRepliesFromOptionsIfNeeded(List.of(), groupsToUse);
        }
      }
      replies = DialogueReplyEngine.ensureExactly3Replies(replies);
    } else {
      replies = DialogueReplyEngine.ensureRepliesFromOptionsIfNeeded(replies, optionGroups);
      replies = DialogueReplyEngine.ensureExactly3Replies(replies);
    }

    DialogueResponse.Memory memory = new DialogueResponse.Memory(
        intent.name().toLowerCase(Locale.ROOT),
        q,
        optionGroups
    );

    Map<String, Object> debug = new LinkedHashMap<>();
    debug.put("mode", dialogueMode.name());
    debug.put("intent", intent.name());
    debug.put("dialogueMode", dialogueMode.name());
    debug.put("llmUsed", usedLlm);

    return new DialogueResponse(intent.name(), replies, optionGroups, memory, debug);
  }

  private static Intent detectIntent(String qLower) {
    if (qLower == null || qLower.isBlank()) return Intent.OTHER;
    String q = qLower;
    if (q.contains("drink") || q.contains("thirsty")) return Intent.DRINK;
    if (containsAny(q, "juice", "water", "milk", "milkshake", "smoothie", "hot chocolate", "squash")) return Intent.DRINK;
    if (q.contains("food") || q.contains("hungry") || q.contains("dinner") || q.contains("lunch") || q.contains("breakfast")
        || q.contains("snack") || q.contains("pizza") || q.contains("something to eat") || q.contains("want to eat")
        || q.contains("like to eat") || q.contains("eat something")) return Intent.FOOD;
    if (containsAny(q, "dog's name", "dogs name", "dog name", "pet's name", "pets name", "pet name", "what is your dog", "whats your dog", "called")) return Intent.PET_NAME;
    if (containsAny(q, "teacher's name", "teachers name", "teacher name", "who is your teacher", "who's your teacher")) return Intent.TEACHER_NAME;
    if (containsAny(q, "dog", "puppy", "cat", "pet", "hamster", "rabbit", "fish", "turtle")) return Intent.PETS;
    if (containsAny(q, "mum", "mom", "mummy", "dad", "daddy", "father", "mother", "sister", "brother", "family", "grandma", "grandpa", "nan", "nanny")) return Intent.FAMILY;
    if (containsAny(q, "play", "game", "ipad", "tablet", "tv", "watch", "outside", "park", "draw", "colour", "color", "music", "song", "dance", "show", "holiday", "place", "favourite place", "place to go")) return Intent.ACTIVITIES;
    if (containsAny(q, "how was school", "how was your day", "how did school go", "how was today")) return Intent.SCHOOL;
    if (q.contains("school")) return Intent.SCHOOL;
    if (containsAny(q, "how are you feeling", "how do you feel", "how are you", "how do you feel today")) return Intent.FEELING;
    if (q.contains("feeling") || q.contains("feel")) return Intent.FEELING;
    if (containsAny(q, "what is your name", "what's your name", "whats your name", "what are you called", "tell me your name", "who are you", "your name")) return Intent.INTRO_REQUEST;
    if (containsAny(q, "my name is", "i'm ", "i am ", "call me ") || looksLikeImNameIntro(q)) return Intent.NAME_INTRO;
    return Intent.OTHER;
  }

  private static boolean looksLikeImNameIntro(String qLower) {
    if (qLower == null || qLower.isBlank()) return false;
    int idx = qLower.indexOf("i'm ");
    int markerLength = 4;
    if (idx < 0) {
      idx = qLower.indexOf("im ");
      markerLength = 3;
    }
    if (idx < 0) return false;
    int start = idx + markerLength;
    if (start >= qLower.length()) return false;
    int end = start;
    while (end < qLower.length() && Character.isLetter(qLower.charAt(end))) {
      end++;
    }
    return end > start;
  }

  private static boolean shouldUseLlm(Intent intent, List<DialogueResponse.Reply> replies) {
    if (intent == null || replies == null || replies.isEmpty()) return true;
    if (intent != Intent.OTHER) return false;

    for (DialogueResponse.Reply r : replies) {
      String kind = r.kind();
      if (kind != null && !kind.equalsIgnoreCase("GENERIC")) {
        return false;
      }
    }
    return true;
  }

  private static boolean containsAny(String haystack, String... needles) {
    if (haystack == null || haystack.isBlank()) return false;
    for (String n : needles) {
      if (n != null && !n.isBlank() && haystack.contains(n)) return true;
    }
    return false;
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
    sb.append("  foods: ").append(DialogueReplyEngine.labelsOnly(foods)).append("\n");
    sb.append("  drinks: ").append(DialogueReplyEngine.labelsOnly(drinks)).append("\n");
    sb.append("  activities: ").append(DialogueReplyEngine.labelsOnly(activities)).append("\n");
    sb.append("  subjects: ").append(DialogueReplyEngine.labelsOnly(subjects)).append(" (subjects & activities from School settings – use for 'What did you do at school today?')\n");
    String byCat = formatActivitiesByCategory(activities);
    if (!byCat.isEmpty()) sb.append(byCat);
    if (favShowFromCarer != null && !favShowFromCarer.isBlank()) {
      sb.append("  carerFavShow: ").append(favShowFromCarer.trim()).append(" (use for 'favourite TV show' when activities lack TV_SHOW)\n");
    }
    sb.append("  pets: ").append(DialogueReplyEngine.labelsOnly(pets)).append("\n");
    sb.append("  teachers: ").append(DialogueReplyEngine.labelsOnly(teachers)).append("\n");
    sb.append("  family: ").append(DialogueReplyEngine.labelsOnly(family)).append("\n\n");

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

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private static String asString(Object value, String fallback) {
    String s = value == null ? "" : value.toString().trim();
    return s.isBlank() ? fallback : s;
  }

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

  private LlmResult generateWithLlm(
      DialogueRequest req,
      UserProfileService.UserProfile profile,
      Intent intent,
      String question,
      List<PreferenceItemEntity> foods,
      List<PreferenceItemEntity> drinks,
      List<PreferenceItemEntity> activities,
      List<PreferenceItemEntity> subjects,
      List<PreferenceItemEntity> pets,
      List<PreferenceItemEntity> teachers,
      List<PreferenceItemEntity> family,
      String favShow
  ) throws Exception {
    String userPrompt = buildUserPrompt(
        safe(req.userName()),
        question,
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

    return new LlmResult(parseReplies(top), parseOptionGroups(groups));
  }

  private record LlmResult(List<DialogueResponse.Reply> replies, List<DialogueResponse.OptionGroup> optionGroups) {
  }
}
