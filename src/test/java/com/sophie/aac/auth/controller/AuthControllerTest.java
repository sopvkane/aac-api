package com.sophie.aac.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.service.AuthService;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.ApiExceptionHandler;
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
  private AuthContext authContext;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    authService = mock(AuthService.class);
    authContext = mock(AuthContext.class);
    when(authContext.currentProfileId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
    UserProfileRepository profileRepo = mock(UserProfileRepository.class);
    var defaultProfile = new com.sophie.aac.profile.domain.UserProfileEntity();
    defaultProfile.setId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
    defaultProfile.setDisplayName("User");
    when(profileRepo.findAllById(any())).thenReturn(java.util.List.of(defaultProfile));

    mvc = MockMvcBuilders
        .standaloneSetup(new AuthController(authService, profileRepo, authContext))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void login_with_pin_sets_httpOnly_cookie_and_returns_role() throws Exception {
    var profileId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(authService.loginWithPin("1234"))
        .thenReturn(new AuthService.LoginResult(Role.PARENT, "token123", 240,
            java.util.List.of(profileId),
            profileId));

    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthController.LoginRequest(null, null, "1234"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("AAC_SESSION=token123")))
        .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
        .andExpect(jsonPath("$.role").value("PARENT"));

    verify(authService).loginWithPin("1234");
  }

  @Test
  void login_with_email_password_sets_cookie_and_returns_role() throws Exception {
    var profileId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(authService.loginWithPassword("user@test.com", "Password1!"))
        .thenReturn(new AuthService.LoginResult(Role.PARENT, "token789", 240,
            java.util.List.of(profileId),
            profileId));

    mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("user@test.com", "Password1!", null))))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("AAC_SESSION=token789")))
        .andExpect(jsonPath("$.role").value("PARENT"));

    verify(authService).loginWithPassword("user@test.com", "Password1!");
  }

  @Test
  void register_creates_account_and_returns_login_response() throws Exception {
    var profileId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(authService.loginWithPassword(eq("new@test.com"), any()))
        .thenReturn(new AuthService.LoginResult(Role.PARENT, "token456", 240,
            java.util.List.of(profileId),
            profileId));

    mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthController.RegisterRequest(
                "New User", "new@test.com", "Password1!@#", "PARENT_CARER", "DEMO2024"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("AAC_SESSION=token456")))
        .andExpect(jsonPath("$.role").value("PARENT"));

    verify(authService).register("New User", "new@test.com", "Password1!@#", "PARENT_CARER", "DEMO2024");
    verify(authService).loginWithPassword("new@test.com", "Password1!@#");
  }

  @Test
  void register_invalid_joining_code_returns_400() throws Exception {
    doThrow(new IllegalArgumentException("Invalid joining code"))
        .when(authService).register(any(), any(), any(), any(), any());

    mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthController.RegisterRequest(
                "User", "u@test.com", "Password1!@#", "PARENT_CARER", "BADCODE"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logout_clears_cookie_even_if_no_cookie_present() throws Exception {
    mvc.perform(post("/api/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("AAC_SESSION=")))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

    // no cookie -> authService.logout not called
    verify(authService).logout(org.mockito.ArgumentMatchers.isNull());
  }

  @Test
  void selectProfile_success_returns_me_response() throws Exception {
    var profileId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(authService.getProfileIdsForSession("token")).thenReturn(java.util.List.of(profileId));
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
