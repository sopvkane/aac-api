package com.sophie.aac.dialogue.service;

import java.util.Locale;

enum DialogueMode {
  SEMANTIC,
  HYBRID,
  LLM;

  static DialogueMode from(String raw) {
    if (raw == null || raw.isBlank()) {
      return SEMANTIC;
    }
    try {
      return DialogueMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return SEMANTIC;
    }
  }
}
