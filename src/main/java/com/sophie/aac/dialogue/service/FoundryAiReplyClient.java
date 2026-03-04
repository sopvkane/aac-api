package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FoundryAiReplyClient implements AiReplyClient {

    private static final Logger log = LoggerFactory.getLogger(FoundryAiReplyClient.class);

    private final String baseUrl;     // e.g. https://aac-project-dev.openai.azure.com/openai/v1/
    private final String apiKey;      // your project/resource key
    private final String model;       // deployment name, e.g. gpt-4.1-mini

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final int maxAttempts;
    private final long retryDelayMs;

    @Autowired
    public FoundryAiReplyClient(
        ObjectMapper mapper,
        @Value("${AZURE_EXISTING_AIPROJECT_ENDPOINT:}") String baseUrl,
        @Value("${AZURE_OPENAI_API_KEY:}") String apiKey,
        @Value("${AZURE_OPENAI_DEPLOYMENT:}") String model,
        @Value("${dialogue.ai.connect-timeout-ms:3000}") long connectTimeoutMs,
        @Value("${dialogue.ai.read-timeout-ms:10000}") long readTimeoutMs,
        @Value("${dialogue.ai.max-attempts:2}") int maxAttempts,
        @Value("${dialogue.ai.retry-delay-ms:150}") long retryDelayMs
    ) {
        this(
            mapper,
            baseUrl,
            apiKey,
            model,
            buildRestTemplate(connectTimeoutMs, readTimeoutMs),
            maxAttempts,
            retryDelayMs
        );
    }

    FoundryAiReplyClient(
        ObjectMapper mapper,
        String baseUrl,
        String apiKey,
        String model,
        RestTemplate restTemplate,
        int maxAttempts,
        long retryDelayMs
    ) {
        String b = baseUrl == null ? "" : baseUrl.trim();
        // ensure ends with /
        if (!b.isBlank() && !b.endsWith("/")) b = b + "/";
        this.baseUrl = b;

        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.mapper = mapper;
        this.restTemplate = restTemplate;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelayMs = Math.max(0L, retryDelayMs);
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
        String raw = executeWithRetry(url, request);

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

    private String executeWithRetry(String url, HttpEntity<Map<String, Object>> request) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                }
                throw new RuntimeException(
                    "Azure OpenAI chat completions failed: HTTP " + response.getStatusCode().value() + " - " + response.getBody()
                );
            } catch (HttpStatusCodeException ex) {
                int status = ex.getStatusCode().value();
                last = new RuntimeException("Azure OpenAI request failed: HTTP " + status + " - " + ex.getResponseBodyAsString(), ex);
                if (status < 500 || attempt == maxAttempts) {
                    throw last;
                }
                log.warn("Azure OpenAI temporary HTTP {} on attempt {}/{}; retrying", status, attempt, maxAttempts);
                sleepBeforeRetry();
            } catch (ResourceAccessException ex) {
                last = new RuntimeException("Azure OpenAI request timeout/connectivity error: " + ex.getMessage(), ex);
                if (attempt == maxAttempts) {
                    throw last;
                }
                log.warn("Azure OpenAI connectivity issue on attempt {}/{}; retrying", attempt, maxAttempts);
                sleepBeforeRetry();
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt == maxAttempts) {
                    throw ex;
                }
                log.warn("Azure OpenAI call failed on attempt {}/{}; retrying", attempt, maxAttempts, ex);
                sleepBeforeRetry();
            }
        }
        if (last != null) {
            throw last;
        }
        throw new RuntimeException("Azure OpenAI chat completions failed");
    }

    private void sleepBeforeRetry() {
        if (retryDelayMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static RestTemplate buildRestTemplate(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(connectTimeoutMs));
        requestFactory.setReadTimeout(Math.toIntExact(readTimeoutMs));
        return new RestTemplate(requestFactory);
    }
}
