package com.sophie.aac.tts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.cognitiveservices.speech.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final String speechKey;
    private final String speechRegion;
    private final String ttsProvider;
    private final String elevenLabsApiKey;
    private final String elevenLabsVoiceId;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_AZURE_VOICE = "en-GB-SoniaNeural";
    private static final String DEFAULT_ELEVENLABS_VOICE = "21m00Tcm4TlvDq8ikWAM"; // Rachel

    public TtsService(
        @Value("${AZURE_SPEECH_KEY:}") String speechKey,
        @Value("${AZURE_SPEECH_REGION:}") String speechRegion,
        @Value("${tts.provider:azure}") String ttsProvider,
        @Value("${ELEVENLABS_API_KEY:}") String elevenLabsApiKey,
        @Value("${ELEVENLABS_VOICE_ID:}") String elevenLabsVoiceId
    ) {
        this.speechKey = speechKey;
        this.speechRegion = speechRegion;
        this.ttsProvider = (ttsProvider == null ? "azure" : ttsProvider.trim().toLowerCase());
        this.elevenLabsApiKey = elevenLabsApiKey == null ? "" : elevenLabsApiKey.trim();
        this.elevenLabsVoiceId = (elevenLabsVoiceId == null || elevenLabsVoiceId.isBlank())
            ? DEFAULT_ELEVENLABS_VOICE
            : elevenLabsVoiceId.trim();

        String active = getActiveProvider();
        log.info("TTS provider: {} (tts.provider={}, ELEVENLABS_API_KEY configured={})",
            active, ttsProvider, !elevenLabsApiKey.isBlank());
    }

    public String getActiveProvider() {
        if ("elevenlabs".equals(ttsProvider) && !elevenLabsApiKey.isBlank()) {
            return "elevenlabs";
        }
        return "azure";
    }

    public byte[] synthesizeMp3(String text, String voiceOverride) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isBlank()) {
            throw new IllegalArgumentException("Text must not be blank.");
        }

        if ("elevenlabs".equals(ttsProvider) && !elevenLabsApiKey.isBlank()) {
            return synthesizeWithElevenLabs(safeText, voiceOverride);
        }

        return synthesizeWithAzure(safeText, voiceOverride);
    }

    private byte[] synthesizeWithElevenLabs(String text, String voiceOverride) {
        String voiceId = (voiceOverride != null && !voiceOverride.isBlank()) ? voiceOverride.trim() : elevenLabsVoiceId;
        // Match quickstart default: mp3_44100_128 (https://elevenlabs.io/docs/eleven-api/quickstart)
        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId + "?output_format=mp3_44100_128";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("xi-api-key", elevenLabsApiKey);
        headers.set("Accept", "audio/mpeg");

        Map<String, Object> body = Map.of(
            "text", text,
            "model_id", "eleven_multilingual_v2"
        );

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("ElevenLabs TTS returned " + response.getStatusCode());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String msg = e.getResponseBodyAsString();
            log.warn("ElevenLabs API error {}: {}", e.getStatusCode(), msg);
            try {
                JsonNode node = objectMapper.readTree(msg);
                if (node.has("detail")) {
                    JsonNode detail = node.get("detail");
                    if (detail.isTextual()) {
                        msg = detail.asText();
                    } else if (detail.isArray() && detail.size() > 0 && detail.get(0).has("msg")) {
                        msg = detail.get(0).get("msg").asText();
                    } else if (detail.has("message")) {
                        msg = detail.get("message").asText();
                    }
                }
            } catch (Exception ignored) {
                // use raw message
            }
            throw new RuntimeException("ElevenLabs TTS failed: " + msg, e);
        } catch (Exception e) {
            throw new RuntimeException("ElevenLabs TTS failed: " + e.getMessage(), e);
        }
    }

    private byte[] synthesizeWithAzure(String text, String voiceOverride) {
        if (speechKey == null || speechKey.isBlank() || speechRegion == null || speechRegion.isBlank()) {
            throw new IllegalStateException(
                "Azure Speech is not configured (AZURE_SPEECH_KEY/AZURE_SPEECH_REGION). " +
                "Set tts.provider=elevenlabs and ELEVENLABS_API_KEY to use ElevenLabs instead."
            );
        }

        String voice = (voiceOverride == null || voiceOverride.isBlank()) ? DEFAULT_AZURE_VOICE : voiceOverride.trim();

        try {
            SpeechConfig config = SpeechConfig.fromSubscription(speechKey, speechRegion);
            config.setSpeechSynthesisVoiceName(voice);
            config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3);

            try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(config)) {
                SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get(10, TimeUnit.SECONDS);

                if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                    return result.getAudioData();
                }

                if (result.getReason() == ResultReason.Canceled) {
                    SpeechSynthesisCancellationDetails details = SpeechSynthesisCancellationDetails.fromResult(result);
                    throw new RuntimeException("TTS canceled: " + details.getReason() + " - " + details.getErrorDetails());
                }

                throw new RuntimeException("TTS failed with reason: " + result.getReason());
            }
        } catch (Exception e) {
            throw new RuntimeException("TTS synthesis failed: " + e.getMessage(), e);
        }
    }
}