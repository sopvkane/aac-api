package com.sophie.aac.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
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

    mvc = MockMvcBuilders
        .standaloneSetup(new AuthController(authService))
        .build();
  }

  @Test
  void login_sets_httpOnly_cookie_and_returns_role() throws Exception {
    when(authService.login(Role.PARENT, "1234"))
        .thenReturn(new AuthService.LoginResult(Role.PARENT, "token123", 240));

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
}