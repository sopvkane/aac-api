package com.sophie.aac.suggestions.service;

import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.repository.PhraseRepository;
import com.sophie.aac.phrases.web.PhraseResponse;
import com.sophie.aac.suggestions.domain.LocationCategory;
import com.sophie.aac.suggestions.domain.TimeBucket;
import com.sophie.aac.suggestions.web.SuggestionItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SuggestionsService {

    private final PhraseRepository phraseRepository;

    public SuggestionsService(PhraseRepository phraseRepository) {
        this.phraseRepository = phraseRepository;
    }

    public List<SuggestionItem> suggest(String prefix, TimeBucket timeBucket, LocationCategory locationCategory, int limit) {
        String p = normalise(prefix);

        // If empty prefix, return a stable small set (alphabetical) for now
        List<PhraseEntity> candidates = new ArrayList<>();
        if (p.isBlank()) {
            candidates.addAll(phraseRepository.findAll(org.springframework.data.domain.Sort.by("text").ascending()));
        } else {
            candidates.addAll(phraseRepository.findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc(p));
            // If not enough, widen search
            if (candidates.size() < 20) {
                candidates.addAll(phraseRepository.findTop50ByTextContainingIgnoreCaseOrderByTextAsc(p));
            }
        }

        // De-dupe by id while preserving order
        candidates = candidates.stream()
            .collect(java.util.stream.Collectors.toMap(
                PhraseEntity::getId,
                e -> e,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ))
            .values()
            .stream()
            .toList();

        return candidates.stream()
            .map(e -> new SuggestionItem(PhraseResponse.from(e), score(e, p, timeBucket, locationCategory)))
            .sorted(Comparator.comparingDouble(SuggestionItem::score).reversed()
                .thenComparing(si -> si.phrase().text(), String.CASE_INSENSITIVE_ORDER))
            .limit(limit)
            .toList();
    }

    private String normalise(String prefix) {
        return prefix == null ? "" : prefix.trim();
    }

    private double score(PhraseEntity e, String prefix, TimeBucket timeBucket, LocationCategory locationCategory) {
        String text = e.getText() == null ? "" : e.getText().toLowerCase();
        String p = prefix == null ? "" : prefix.toLowerCase();

        double prefixScore = 0.0;
        if (!p.isBlank()) {
            if (text.startsWith(p)) prefixScore = 1.0;
            else if (text.contains(p)) prefixScore = 0.6;
        } else {
            prefixScore = 0.2; // stable default when empty
        }

        // Context boosts can be wired later; keep small for determinism
        double contextBoost = 0.0;
        // Example: if you're at HOME in MORNING, you might boost "breakfast" category later.
        // For AAC-10 we keep it at 0 to avoid making assumptions.

        return prefixScore + contextBoost;
    }
}