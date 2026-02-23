package com.sophie.aac.speech.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SpeechTokenService {

    private final String speechKey;
    private final String speechRegion;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ReentrantLock lock = new ReentrantLock();
    private volatile CachedToken cached;

    public SpeechTokenService(
        @Value("${AZURE_SPEECH_KEY:}") String speechKey,
        @Value("${AZURE_SPEECH_REGION:}") String speechRegion
    ) {
        this.speechKey = speechKey;
        this.speechRegion = speechRegion;
    }

    public TokenResponse getToken() {
        if (speechKey == null || speechKey.isBlank() || speechRegion == null || speechRegion.isBlank()) {
            throw new IllegalStateException("Azure Speech is not configured (AZURE_SPEECH_KEY/AZURE_SPEECH_REGION).");
        }

        CachedToken current = cached;
        if (current != null && current.isValid()) {
            return current.toResponse(speechRegion);
        }

        lock.lock();
        try {
            current = cached;
            if (current != null && current.isValid()) {
                return current.toResponse(speechRegion);
            }

            String url = "https://" + speechRegion + ".api.cognitive.microsoft.com/sts/v1.0/issueToken";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Ocp-Apim-Subscription-Key", speechKey);

            // CRITICAL: Azure is requiring explicit content length.
            headers.setContentLength(0);

            // Some gateways also like a content-type even if body is empty.
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>("", headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Speech token request failed: HTTP " + response.getStatusCode());
            }

            String token = response.getBody();
            if (token == null || token.isBlank()) {
                throw new RuntimeException("Speech token request returned empty token.");
            }

            Instant expiresAt = Instant.now().plusSeconds(9 * 60);
            cached = new CachedToken(token.trim(), expiresAt);

            return cached.toResponse(speechRegion);

        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Speech token: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }

        TokenResponse toResponse(String region) {
            long secondsLeft = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
            return new TokenResponse(token, region, secondsLeft);
        }
    }

    public record TokenResponse(String token, String region, long expiresInSeconds) {}
}