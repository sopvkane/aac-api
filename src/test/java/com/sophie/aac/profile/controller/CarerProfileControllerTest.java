package com.sophie.aac.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import com.sophie.aac.profile.service.ProfileSetupService;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.web.CreateProfileRequest;
import com.sophie.aac.profile.web.UpdateUserProfileRequest;
import com.sophie.aac.profile.web.UserProfileResponse;
import com.sophie.aac.suggestions.domain.LocationCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
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
  private ProfileSetupService profileSetupService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = mock(CaregiverProfileService.class);
    profileSetupService = mock(ProfileSetupService.class);
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mvc = MockMvcBuilders.standaloneSetup(new CarerProfileController(service, profileSetupService))
        .setValidator(validator)
        .build();
    // Simulate PARENT role so profile update is allowed (only PARENT/CLINICIAN can update)
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("test", "n/a",
            AuthorityUtils.createAuthorityList("ROLE_PARENT")));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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
        null, null, null, null, null, null, null, null, null, null, null, null, null, null
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

  @Test
  void update_returns_403_for_carer_role() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("carer", "n/a",
            AuthorityUtils.createAuthorityList("ROLE_CARER")));

    UpdateUserProfileRequest req = new UpdateUserProfileRequest(
        "New Name", "Hey", true, false, true, true, true, LocationCategory.HOME,
        true, true, false, true, 3,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null
    );

    mvc.perform(put("/api/carer/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isForbidden());

    verify(service, never()).update(any());
  }

  @Test
  void createProfile_returns_201_and_new_profile() throws Exception {
    var created = new UserProfileEntity();
    created.setId(UUID.randomUUID());
    created.setDisplayName("Mia");
    created.setWakeName("Hey Mia");
    when(profileSetupService.createProfile("Mia", "Hey Mia")).thenReturn(created);

    mvc.perform(post("/api/carer/profiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateProfileRequest("Mia", "Hey Mia"))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/carer/profile"))
        .andExpect(jsonPath("$.displayName").value("Mia"))
        .andExpect(jsonPath("$.wakeName").value("Hey Mia"));

    verify(profileSetupService).createProfile("Mia", "Hey Mia");
  }
}
