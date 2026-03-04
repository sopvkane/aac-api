package com.sophie.aac.dialogue.service;

import com.sophie.aac.dialogue.web.DialogueResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueReplySelectionTest {

  @Test
  void generateRepliesFromQuestionOptions_parses_or_question_without_regex_path() {
    DialogueReplyEngine.Result result =
        DialogueReplySelection.generateRepliesFromQuestionOptions("would you like a juice or some water?");

    assertThat(result).isNotNull();
    assertThat(result.replies()).hasSize(2);
    assertThat(result.replies()).extracting(DialogueResponse.Reply::label)
        .containsExactly("Juice", "Water");
  }

  @Test
  void generateRepliesFromQuestionOptions_handles_of_suffix_cleanup() {
    DialogueReplyEngine.Result result =
        DialogueReplySelection.generateRepliesFromQuestionOptions("would you like a cup of tea or a bowl of cereal?");

    assertThat(result).isNotNull();
    assertThat(result.replies()).extracting(DialogueResponse.Reply::label)
        .contains("Cup", "Bowl");
  }
}
