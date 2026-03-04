package com.sophie.aac.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.service.DelegatedPinService;
import com.sophie.aac.auth.util.TestSecurityHelper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DelegatedPinControllerTest {

  private final DelegatedPinService delegatedPinService = mock(DelegatedPinService.class);
  private final MockMvc mvc = MockMvcBuilders
      .standaloneSetup(new DelegatedPinController(delegatedPinService))
      .build();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @AfterEach
  void tearDown() {
    TestSecurityHelper.clear();
  }

  @Test
  void create_returns_pin_summary() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID profileId = TestSecurityHelper.DEFAULT_PROFILE_ID;
    TestSecurityHelper.setUserWithProfile(userId);

    DelegatedPinEntity entity = new DelegatedPinEntity();
    UUID pinId = UUID.randomUUID();
    entity.setId(pinId);
    entity.setLabel("Grandma");
    entity.setCreatedByUserId(userId);
    entity.setProfileId(profileId);
    entity.setCreatedAt(Instant.now());
    entity.setActive(true);
    when(delegatedPinService.create(eq("Grandma"), eq("1234"), eq(profileId))).thenReturn(entity);

    mvc.perform(post("/api/auth/pins")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new DelegatedPinController.CreatePinRequest("1234", "Grandma", profileId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Grandma"))
        .andExpect(jsonPath("$.profileId").value(profileId.toString()));

    verify(delegatedPinService).create("Grandma", "1234", profileId);
  }

  @Test
  void list_returns_pin_summaries() throws Exception {
    UUID userId = UUID.randomUUID();
    TestSecurityHelper.setUserWithProfile(userId);

    DelegatedPinEntity e = new DelegatedPinEntity();
    e.setId(UUID.randomUUID());
    e.setCreatedByUserId(userId);
    e.setLabel("Teacher");
    e.setProfileId(TestSecurityHelper.DEFAULT_PROFILE_ID);
    e.setCreatedAt(Instant.now());
    e.setActive(true);
    when(delegatedPinService.listByCurrentUser()).thenReturn(List.of(e));

    mvc.perform(get("/api/auth/pins"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Teacher"));

    verify(delegatedPinService).listByCurrentUser();
  }

  @Test
  void revoke_deletes_pin() throws Exception {
    UUID pinId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    TestSecurityHelper.setUserWithProfile(userId);

    mvc.perform(delete("/api/auth/pins/" + pinId))
        .andExpect(status().isNoContent());

    verify(delegatedPinService).revoke(pinId);
  }
}
