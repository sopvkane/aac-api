package com.sophie.aac.dialogue.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DialogueReplyCommonTest {

  @Test
  void withArticle_keeps_numeric_quantities_without_article() {
    assertThat(DialogueReplyCommon.withArticle("Two biscuits")).isEqualTo("Two biscuits");
    assertThat(DialogueReplyCommon.withArticle("2 cookies")).isEqualTo("2 cookies");
  }

  @Test
  void withArticle_adds_expected_article_for_simple_items() {
    assertThat(DialogueReplyCommon.withArticle("apple")).isEqualTo("an apple");
    assertThat(DialogueReplyCommon.withArticle("banana")).isEqualTo("a banana");
  }
}
