package com.sophie.aac.suggestions.web;

import com.sophie.aac.suggestions.domain.LocationCategory;
import com.sophie.aac.suggestions.domain.TimeBucket;

public record SuggestionsRequest(
    String prefix,
    TimeBucket timeBucket,
    LocationCategory locationCategory
) {}