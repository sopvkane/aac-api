package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DialogueService {

    private final AiReplyClient ai;
    private final ObjectMapper mapper;

    public DialogueService(AiReplyClient ai) {
        this.ai = ai;
        this.mapper = new ObjectMapper();
    }

    public DialogueResponse generateReplies(DialogueRequest req) {
        String name = safe(req.userName());
        String q = safe(req.questionText());

        if (!ai.isConfigured()) {
            return fallback("AI_NOT_CONFIGURED", null);
        }

        // Stronger instruction: JSON only, single object, no code fences
        String system = """
You are an AAC assistant. You MUST output STRICT JSON ONLY.
No markdown. No prose. No code fences. No leading/trailing text.

Return a SINGLE JSON object with this schema:

{
  "intent": "drink_offer|food_offer|yes_no_general|help_request|bathroom|pain|other",
  "topReplies": [
    {"id":"yes","label":"Yes","text":"..."},
    {"id":"no","label":"No","text":"..."},
    {"id":"instead","label":"Instead","text":"..."}
  ],
  "optionGroups": [
    {"id":"group1","title":"More options","items":["item1","item2","item3"]}
  ]
}

Rules:
- ALWAYS output intent and topReplies with EXACTLY 3 items.
- Each reply text must be a polite, complete sentence (AAC-friendly).
- optionGroups may be [] if not relevant.
- Keep items short nouns (e.g., "water", "sandwich", "toilet").
- Output must start with '{' and end with '}'.
""";

        String user = "User name: " + name + "\nQuestion: " + q;

        try {
            String content = ai.generateJson(system, user);
            if (content == null || content.isBlank()) {
                return fallback("AI_EMPTY", null);
            }

            // Robust: if model outputs extra text, extract JSON object
            String extracted = extractFirstJsonObject(content);

            Map<String, Object> root = mapper.readValue(extracted, new TypeReference<Map<String, Object>>() {});
            String intent = asString(root.get("intent"), "other");

            List<Map<String, Object>> top = asListOfMaps(root.get("topReplies"));
            List<Map<String, Object>> groups = asListOfMaps(root.get("optionGroups"));

            List<DialogueResponse.Reply> replies = new ArrayList<>();
            for (Map<String, Object> r : top) {
                replies.add(new DialogueResponse.Reply(
                    asString(r.get("id"), UUID.randomUUID().toString()),
                    asString(r.get("label"), "Option"),
                    asString(r.get("text"), "")
                ));
            }
            replies = normalize3(replies);

            List<DialogueResponse.OptionGroup> optionGroups = new ArrayList<>();
            for (Map<String, Object> g : groups) {
                List<String> items = asStringList(g.get("items"));
                optionGroups.add(new DialogueResponse.OptionGroup(
                    asString(g.get("id"), "options"),
                    asString(g.get("title"), "More options"),
                    items
                ));
            }

            return new DialogueResponse(
                intent,
                replies,
                optionGroups,
                Map.of("mode", "AI")
            );
        } catch (Exception e) {
            // CRITICAL: surface the real reason so you can fix config quickly
            return fallback("AI_PARSE_OR_CALL_FAIL", e.getMessage());
        }
    }

    private DialogueResponse fallback(String reason, String error) {
        var replies = List.of(
            new DialogueResponse.Reply("yes", "Yes", "Yes please."),
            new DialogueResponse.Reply("no", "No", "No thank you."),
            new DialogueResponse.Reply("repeat", "Repeat", "Could you say that again, please?")
        );

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("mode", "FALLBACK");
        debug.put("reason", reason);
        if (error != null && !error.isBlank()) debug.put("error", error);

        return new DialogueResponse(
            "other",
            replies,
            List.of(),
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
                    for (Map.Entry<?, ?> e : m.entrySet()) {
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

    private static List<DialogueResponse.Reply> normalize3(List<DialogueResponse.Reply> in) {
        List<DialogueResponse.Reply> out = new ArrayList<>(in == null ? List.of() : in);

        while (out.size() < 3) {
            int idx = out.size();
            out.add(new DialogueResponse.Reply(
                "fallback-" + idx,
                idx == 0 ? "Yes" : idx == 1 ? "No" : "Repeat",
                idx == 0 ? "Yes please." : idx == 1 ? "No thank you." : "Could you say that again, please?"
            ));
        }

        return out.subList(0, 3);
    }

    private static String extractFirstJsonObject(String s) {
        if (s == null) return "";
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s.trim();
    }
}