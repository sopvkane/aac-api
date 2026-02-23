package com.sophie.aac.dialogue.web;

import java.util.Map;

public record DialogueRequest(
    String userName,
    String questionText,
    Map<String, String> context
) {}