package com.sophie.aac.dialogue.web;

import java.util.List;
import java.util.Map;

public record DialogueResponse(
        String intent,
        List<Reply> topReplies,
        List<OptionGroup> optionGroups,
        Memory memory,
        Map<String, Object> debug
) {
    /** If true, frontend should show "Who do you want to ask?" before speaking. */
    public record Reply(String id, String label, String text, String iconUrl, String kind, boolean requiresPersonSelection) {
        /** Convenience: requiresPersonSelection=true for DRINK/FOOD/ACTIVITY requests. */
        public Reply(String id, String label, String text, String iconUrl, String kind) {
            this(id, label, text, iconUrl, kind,
                "DRINK".equals(kind) || "FOOD".equals(kind) || "ACTIVITY".equals(kind));
        }
    }

    public record OptionGroup(String id, String title, List<String> items, List<OptionItem> itemsWithIcons) {
        public OptionGroup(String id, String title, List<String> items) {
            this(id, title, items, null);
        }
    }
    /** Label + iconUrl for sub-options. Use when iconUrl available – display icons large for accessibility. */
    public record OptionItem(String label, String iconUrl) {}

    public record Memory(
            String lastIntent,
            String lastQuestionText,
            List<OptionGroup> lastOptionGroups
    ) {}
}