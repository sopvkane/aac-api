package com.sophie.aac.suggestions.web;

import com.sophie.aac.suggestions.domain.LocationCategory;
import com.sophie.aac.suggestions.domain.TimeBucket;

import java.util.List;

public record SuggestionsResponse(
    List<SuggestionItem> suggestions,
    Meta meta
) {
    public record Meta(String prefix, TimeBucket timeBucket, LocationCategory locationCategory, int limit) {}
}