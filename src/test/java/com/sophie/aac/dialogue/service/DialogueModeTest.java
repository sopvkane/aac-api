package com.sophie.aac.dialogue.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueModeTest {

  @Test
  void from_handles_valid_values_case_insensitively() {
    assertThat(DialogueMode.from("llm")).isEqualTo(DialogueMode.LLM);
    assertThat(DialogueMode.from("Hybrid")).isEqualTo(DialogueMode.HYBRID);
  }

  @Test
  void from_defaults_to_semantic_for_invalid_or_blank_values() {
    assertThat(DialogueMode.from("")).isEqualTo(DialogueMode.SEMANTIC);
    assertThat(DialogueMode.from("unknown")).isEqualTo(DialogueMode.SEMANTIC);
    assertThat(DialogueMode.from(null)).isEqualTo(DialogueMode.SEMANTIC);
  }
}
