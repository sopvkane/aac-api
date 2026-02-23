package com.sophie.aac.dialogue.service;

public interface AiReplyClient {
    String generateJson(String systemPrompt, String userPrompt);
    boolean isConfigured();
}