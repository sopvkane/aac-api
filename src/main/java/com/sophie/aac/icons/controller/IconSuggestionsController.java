package com.sophie.aac.icons.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Suggested icon paths for common labels. Use when iconUrl is null on phrases/preference items.
 * Frontend should resolve paths relative to its icon assets (e.g. /icons/drink/water.svg).
 * Display icons large for accessibility – users may not be able to read.
 */
@RestController
@RequestMapping("/api/icons")
public class IconSuggestionsController {

    private static final Map<String, String> SUGGESTIONS = Map.ofEntries(
        Map.entry("Water", "icons/drink/water"),
        Map.entry("Juice", "icons/drink/juice"),
        Map.entry("Milk", "icons/drink/milk"),
        Map.entry("Tea", "icons/drink/tea"),
        Map.entry("Coffee", "icons/drink/coffee"),
        Map.entry("Orange Juice", "icons/drink/juice"),
        Map.entry("Apple Juice", "icons/drink/juice"),
        Map.entry("Toast", "icons/food/toast"),
        Map.entry("Banana", "icons/food/banana"),
        Map.entry("Sandwich", "icons/food/sandwich"),
        Map.entry("Pizza", "icons/food/pizza"),
        Map.entry("Cereal", "icons/food/cereal"),
        Map.entry("Apple", "icons/food/apple"),
        Map.entry("Toilet", "icons/needs/toilet"),
        Map.entry("Help", "icons/needs/help"),
        Map.entry("Hurt", "icons/needs/hurt"),
        Map.entry("Tired", "icons/needs/tired"),
        Map.entry("Mum", "icons/people/mum"),
        Map.entry("Dad", "icons/people/dad"),
        Map.entry("Grandma", "icons/people/grandma"),
        Map.entry("Grandad", "icons/people/grandad"),
        Map.entry("Mrs Patel", "icons/people/teacher"),
        Map.entry("Mr Jones", "icons/people/teacher"),
        Map.entry("Bluey", "icons/activity/bluey"),
        Map.entry("TV", "icons/activity/tv"),
        Map.entry("Play", "icons/activity/play"),
        Map.entry("iPad", "icons/activity/ipad"),
        Map.entry("Outside", "icons/activity/outside"),
        Map.entry("Watch Bluey", "icons/activity/bluey"),
        Map.entry("Food", "icons/category/food"),
        Map.entry("Drink", "icons/category/drink"),
        Map.entry("Yes", "icons/response/yes"),
        Map.entry("No", "icons/response/no"),
        Map.entry("Just say it (no name)", "icons/category/speak")
    );

    /** Label/category → suggested icon path. Paths are relative; frontend resolves to actual asset. */
    @GetMapping("/suggestions")
    public Map<String, Object> suggestions() {
        return Map.of(
            "preferredSize", "large",
            "suggestions", SUGGESTIONS,
            "categories", List.of("drink", "food", "needs", "people", "activity", "response")
        );
    }

    /** Look up icon path for a single label. Case-insensitive; returns null if no match. */
    @GetMapping("/suggest")
    public Map<String, String> suggest(@RequestParam String label) {
        String trimmed = label == null ? "" : label.trim();
        if (trimmed.isEmpty()) {
            return Map.of("label", "", "iconPath", "");
        }
        String exact = SUGGESTIONS.get(trimmed);
        if (exact != null) {
            return Map.of("label", trimmed, "iconPath", exact);
        }
        String match = SUGGESTIONS.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(trimmed))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(() -> SUGGESTIONS.entrySet().stream()
                .filter(e -> trimmed.toLowerCase().contains(e.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null));
        return Map.of("label", trimmed, "iconPath", match != null ? match : "");
    }
}
