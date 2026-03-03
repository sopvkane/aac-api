package com.sophie.aac.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import com.sophie.aac.profile.web.UpdateUserProfileRequest;
import com.sophie.aac.profile.web.UserProfileResponse;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarerProfileControllerTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private CaregiverProfileService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = mock(CaregiverProfileService.class);
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mvc = MockMvcBuilders.standaloneSetup(new CarerProfileController(service))
        .setValidator(validator)
        .build();
  }

  @Test
  void get_returns_profile() throws Exception {
    UserProfileEntity p = new UserProfileEntity();
    p.setId(UUID.randomUUID());
    p.setDisplayName("Test User");
    p.setWakeName("Hey");
    p.setDefaultLocation(LocationCategory.HOME);
    when(service.get()).thenReturn(p);

    mvc.perform(get("/api/carer/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Test User"));

    verify(service).get();
  }

  @Test
  void update_calls_service_and_returns_updated() throws Exception {
    UpdateUserProfileRequest req = new UpdateUserProfileRequest(
        "New Name", "Hey", true, false, true, true, true, LocationCategory.HOME,
        true, true, false, true, 3,
        null, null, null, null, null, null, null, null, null, null, null, null, null
    );
    UserProfileEntity updated = new UserProfileEntity();
    updated.setDisplayName("New Name");
    when(service.update(any(UpdateUserProfileRequest.class))).thenReturn(updated);

    mvc.perform(put("/api/carer/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("New Name"));

    verify(service).update(any(UpdateUserProfileRequest.class));
  }
}
