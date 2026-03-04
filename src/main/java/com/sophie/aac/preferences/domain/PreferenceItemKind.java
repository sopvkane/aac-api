package com.sophie.aac.preferences.domain;

import java.util.Locale;
import java.util.Optional;

public enum PreferenceItemKind {
  FOOD,
  DRINK,
  ACTIVITY,
  TEACHER,
  BUS_STAFF,
  SCHOOL_PEER,
  SUBJECT,
  SCHOOL_ACTIVITY,
  FAMILY_MEMBER,
  PET,
  EMOTION;

  public static Optional<PreferenceItemKind> from(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
