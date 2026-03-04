package com.sophie.aac.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.service.AuthService;
import com.sophie.aac.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    authService = mock(AuthService.class);
    UserProfileRepository profileRepo = mock(UserProfileRepository.class);
    var defaultProfile = new com.sophie.aac.profile.domain.UserProfileEntity();
    defaultProfile.setId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
    defaultProfile.setDisplayName("User");
    when(profileRepo.findAllById(any())).thenReturn(java.util.List.of(defaultProfile));

    mvc = MockMvcBuilders
        .standaloneSetup(new AuthController(authService, profileRepo))
        .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void login_sets_httpOnly_cookie_and_returns_role() throws Exception {
    when(authService.login(Role.PARENT, "1234"))
        .thenReturn(new AuthService.LoginResult(Role.PARENT, "token123", 240,
            java.util.List.of(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")));

    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthController.LoginRequest(Role.PARENT, "1234"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("AAC_SESSION=token123")))
        .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
        .andExpect(jsonPath("$.role").value("PARENT"));

    verify(authService).login(Role.PARENT, "1234");
  }

  @Test
  void logout_clears_cookie_even_if_no_cookie_present() throws Exception {
    mvc.perform(post("/api/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("AAC_SESSION=")))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

    // no cookie -> authService.logout not called
    verify(authService).logout(null);
  }

  @Test
  void selectProfile_success_returns_me_response() throws Exception {
    var profileId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(authService.getProfileIdsForRole(Role.PARENT)).thenReturn(java.util.List.of(profileId));
    var auth = new UsernamePasswordAuthenticationToken("parent", null,
        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PARENT")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mvc.perform(post("/api/auth/select-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new jakarta.servlet.http.Cookie("AAC_SESSION", "token"))
            .content(objectMapper.writeValueAsString(new AuthController.SelectProfileRequest(profileId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PARENT"))
        .andExpect(jsonPath("$.activeProfileId").value(profileId.toString()))
        .andExpect(jsonPath("$.profileIds").isArray())
        .andExpect(jsonPath("$.profiles").isArray());

    verify(authService).selectProfile("token", profileId);
  }

  @Test
  void selectProfile_forbidden_when_no_access_to_profile() throws Exception {
    var profileId = java.util.UUID.randomUUID();
    doThrow(new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.FORBIDDEN, "No access to profile"))
        .when(authService).selectProfile(any(), eq(profileId));

    var auth = new UsernamePasswordAuthenticationToken("parent", null,
        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PARENT")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mvc.perform(post("/api/auth/select-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new jakarta.servlet.http.Cookie("AAC_SESSION", "token"))
            .content(objectMapper.writeValueAsString(new AuthController.SelectProfileRequest(profileId))))
        .andExpect(status().isForbidden());

    verify(authService).selectProfile("token", profileId);
  }
}