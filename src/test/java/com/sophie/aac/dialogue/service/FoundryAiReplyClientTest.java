package com.sophie.aac.dialogue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FoundryAiReplyClientTest {

  @Test
  void generateJson_returns_empty_when_not_configured() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    FoundryAiReplyClient client = new FoundryAiReplyClient(
        new ObjectMapper(),
        "",
        "key",
        "gpt-test",
        restTemplate,
        2,
        0
    );

    assertThat(client.isConfigured()).isFalse();
    assertThat(client.generateJson("system", "user")).isEmpty();
    verifyNoInteractions(restTemplate);
  }

  @Test
  void generateJson_retries_on_server_error_and_then_returns_content() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
        .thenReturn(ResponseEntity.ok("""
            {
              "choices": [
                {"message": {"content": "{\\"topReplies\\":[] , \\"optionGroups\\":[] }"}}
              ]
            }
            """));

    FoundryAiReplyClient client = new FoundryAiReplyClient(
        new ObjectMapper(),
        "https://example.test/openai/v1/",
        "key",
        "gpt-test",
        restTemplate,
        2,
        0
    );

    String json = client.generateJson("system", "user");

    assertThat(json).contains("topReplies");
    verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class));
  }

  @Test
  void generateJson_does_not_retry_on_client_error() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    FoundryAiReplyClient client = new FoundryAiReplyClient(
        new ObjectMapper(),
        "https://example.test/openai/v1/",
        "key",
        "gpt-test",
        restTemplate,
        3,
        0
    );

    assertThatThrownBy(() -> client.generateJson("system", "user"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("HTTP 400");

    verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class));
  }

  @Test
  void generateJson_retries_on_connectivity_error_and_then_returns_content() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(new ResourceAccessException("timeout"))
        .thenReturn(ResponseEntity.ok("""
            {"choices":[{"message":{"content":"{\\"topReplies\\":[] , \\"optionGroups\\":[] }"}}]}
            """));

    FoundryAiReplyClient client = new FoundryAiReplyClient(
        new ObjectMapper(),
        "https://example.test/openai/v1/",
        "key",
        "gpt-test",
        restTemplate,
        2,
        0
    );

    String json = client.generateJson("system", "user");
    assertThat(json).contains("optionGroups");
    verify(restTemplate, times(2)).exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class));
  }

  @Test
  void generateJson_throws_when_response_body_is_empty() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(""));

    FoundryAiReplyClient client = new FoundryAiReplyClient(
        new ObjectMapper(),
        "https://example.test/openai/v1/",
        "key",
        "gpt-test",
        restTemplate,
        1,
        0
    );

    assertThatThrownBy(() -> client.generateJson("system", "user"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("empty body");
  }

  @Test
  void generateJson_throws_when_response_payload_is_not_json() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("not-json"));

    FoundryAiReplyClient client = new FoundryAiReplyClient(
        new ObjectMapper(),
        "https://example.test/openai/v1/",
        "key",
        "gpt-test",
        restTemplate,
        1,
        0
    );

    assertThatThrownBy(() -> client.generateJson("system", "user"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to parse");
  }
}
