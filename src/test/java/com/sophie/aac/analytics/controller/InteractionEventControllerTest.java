package com.sophie.aac.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.analytics.service.InteractionEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InteractionEventControllerTest {

  private MockMvc mvc;
  private InteractionEventService service;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    service = mock(InteractionEventService.class);
    mapper = new ObjectMapper();
    mvc = MockMvcBuilders.standaloneSetup(new InteractionEventController(service)).build();
  }

  @Test
  void record_maps_known_location_values() throws Exception {
    var body = new InteractionEventController.RecordRequest("OPTION_SELECTED", "school", "QUICK", "Water");

    mvc.perform(post("/api/interactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(body)))
        .andExpect(status().isOk());

    verify(service).record("OPTION_SELECTED", com.sophie.aac.suggestions.domain.LocationCategory.SCHOOL, "QUICK", "Water");
  }

  @Test
  void record_maps_bus_to_home() throws Exception {
    var body = new InteractionEventController.RecordRequest("OPTION_SELECTED", "BUS", "QUICK", "Snack");

    mvc.perform(post("/api/interactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(body)))
        .andExpect(status().isOk());

    verify(service).record("OPTION_SELECTED", com.sophie.aac.suggestions.domain.LocationCategory.HOME, "QUICK", "Snack");
  }

  @Test
  void record_defaults_location_to_home_when_blank_or_unknown() throws Exception {
    var blank = new InteractionEventController.RecordRequest("OPTION_SELECTED", " ", "QUICK", "Tea");
    mvc.perform(post("/api/interactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(blank)))
        .andExpect(status().isOk());
    verify(service).record("OPTION_SELECTED", com.sophie.aac.suggestions.domain.LocationCategory.HOME, "QUICK", "Tea");

    var unknown = new InteractionEventController.RecordRequest("OPTION_SELECTED", "mars", "QUICK", "Toast");
    mvc.perform(post("/api/interactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(unknown)))
        .andExpect(status().isOk());
    verify(service).record("OPTION_SELECTED", com.sophie.aac.suggestions.domain.LocationCategory.HOME, "QUICK", "Toast");
  }
}

