package com.sophie.aac.profile.web;

import com.sophie.aac.suggestions.domain.LocationCategory;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String displayName,
    String wakeName,
    boolean detailsDefault,
    boolean voiceDefault,
    boolean aiEnabled,
    boolean memoryEnabled,
    boolean analyticsEnabled,
    LocationCategory defaultLocation,
    boolean allowHome,
    boolean allowSchool,
    boolean allowWork,
    boolean allowOther,
    int maxOptions,
    String favFood,
    String favDrink,
    String favShow,
    String favTopic,
    Instant updatedAt
) {}