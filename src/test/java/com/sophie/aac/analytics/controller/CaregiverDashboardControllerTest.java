package com.sophie.aac.analytics.controller;

import com.sophie.aac.analytics.service.CaregiverDashboardService;
import com.sophie.aac.analytics.web.CaregiverDashboardResponse;
import com.sophie.aac.suggestions.domain.TimeBucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CaregiverDashboardControllerTest {

  private MockMvc mvc;
  private CaregiverDashboardService service;

  @BeforeEach
  void setUp() {
    service = mock(CaregiverDashboardService.class);
    mvc = MockMvcBuilders.standaloneSetup(new CaregiverDashboardController(service)).build();
  }

  @Test
  void get_returns_dashboard_response() throws Exception {
    Map<TimeBucket, Long> buckets = new EnumMap<>(TimeBucket.class);
    for (TimeBucket b : TimeBucket.values()) {
      buckets.put(b, 0L);
    }
    CaregiverDashboardResponse resp = new CaregiverDashboardResponse(
        "WEEK", Instant.now(), "User", "Apple", "Juice", "Show", buckets,
        10L, 2L, 5L, 1L, 4.0, Map.of("HEAD", 1L)
    );
    when(service.getDashboard("WEEK")).thenReturn(resp);

    mvc.perform(get("/api/carer/dashboard").param("period", "WEEK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("User"))
        .andExpect(jsonPath("$.period").value("WEEK"));

    verify(service).getDashboard("WEEK");
  }

  @Test
  void get_uses_default_period() throws Exception {
    Map<TimeBucket, Long> buckets = new EnumMap<>(TimeBucket.class);
    for (TimeBucket b : TimeBucket.values()) buckets.put(b, 0L);
    CaregiverDashboardResponse resp = new CaregiverDashboardResponse(
        "WEEK", Instant.now(), "U", null, null, null, buckets, 0L, 0L, 0L, 0L, null, Map.of()
    );
    when(service.getDashboard("WEEK")).thenReturn(resp);

    mvc.perform(get("/api/carer/dashboard"))
        .andExpect(status().isOk());

    verify(service).getDashboard("WEEK");
  }
}
