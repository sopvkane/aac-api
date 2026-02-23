package com.sophie.aac.speech.controller;

import com.sophie.aac.speech.service.SpeechTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SpeechTokenControllerTest {

    private MockMvc mvc;
    private SpeechTokenService service;

    @BeforeEach
    void setUp() {
        service = mock(SpeechTokenService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SpeechTokenController(service)).build();
    }

    @Test
    void returns_token_payload() throws Exception {
        when(service.getToken()).thenReturn(new SpeechTokenService.TokenResponse("token123", "swedencentral", 540));

        mvc.perform(get("/api/speech/token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token123"))
            .andExpect(jsonPath("$.region").value("swedencentral"))
            .andExpect(jsonPath("$.expiresInSeconds").value(540));

        verify(service).getToken();
    }
}