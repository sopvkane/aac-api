package com.sophie.aac.wellbeing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.wellbeing.service.WellbeingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WellbeingControllerTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private WellbeingService wellbeingService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    wellbeingService = mock(WellbeingService.class);
    mvc = MockMvcBuilders.standaloneSetup(new WellbeingController(wellbeingService)).build();
  }

  @Test
  void recordMood_calls_service() throws Exception {
    mvc.perform(post("/api/wellbeing/mood")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"moodScore\": 3}"))
        .andExpect(status().isOk());

    verify(wellbeingService).recordMood(3);
  }

  @Test
  void recordPain_calls_service() throws Exception {
    mvc.perform(post("/api/wellbeing/pain")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"bodyArea\": \"head\", \"severity\": 5, \"notes\": \"migraine\"}"))
        .andExpect(status().isOk());

    verify(wellbeingService).recordPain("head", 5, "migraine");
  }
}
