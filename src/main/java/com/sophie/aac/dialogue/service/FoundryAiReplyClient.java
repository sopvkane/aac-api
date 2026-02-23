package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class FoundryAiReplyClient implements AiReplyClient {

    private final String baseUrl;     // e.g. https://aac-project-dev.openai.azure.com/openai/v1/
    private final String apiKey;      // your project/resource key
    private final String model;       // deployment name, e.g. gpt-4.1-mini

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public FoundryAiReplyClient(
        @Value("${AZURE_EXISTING_AIPROJECT_ENDPOINT:}") String baseUrl,
        @Value("${AZURE_OPENAI_API_KEY:}") String apiKey,
        @Value("${AZURE_OPENAI_DEPLOYMENT:}") String model
    ) {
        String b = baseUrl == null ? "" : baseUrl.trim();
        // ensure ends with /
        if (!b.isBlank() && !b.endsWith("/")) b = b + "/";
        this.baseUrl = b;

        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isBlank() && !apiKey.isBlank() && !model.isBlank();
    }

    @Override
    public String generateJson(String systemPrompt, String userPrompt) {
        if (!isConfigured()) return "";

        // OpenAI-compatible route under /openai/v1/
        String url = baseUrl + "chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        body.put("temperature", 0.2);
        body.put("max_tokens", 450);

        // Encourage JSON-only output where supported
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                "Azure OpenAI chat completions failed: HTTP " + response.getStatusCode().value() + " - " + response.getBody()
            );
        }

        String raw = response.getBody();
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("Azure OpenAI returned empty body.");
        }

        // Extract assistant content: choices[0].message.content
        try {
            Map<String, Object> root = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            Object choicesObj = root.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "";

            Object firstObj = choices.get(0);
            if (!(firstObj instanceof Map<?, ?> firstMap)) return "";

            Object messageObj = firstMap.get("message");
            if (!(messageObj instanceof Map<?, ?> messageMap)) return "";

            Object contentObj = messageMap.get("content");
            return contentObj == null ? "" : contentObj.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Azure OpenAI response: " + e.getMessage(), e);
        }
    }
}