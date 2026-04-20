package com.sophie.aac.icons.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IconSuggestionsControllerTest {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new IconSuggestionsController()).build();
  }

  @Test
  void suggestions_returns_expected_shape() throws Exception {
    mvc.perform(get("/api/icons/suggestions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.preferredSize").value("large"))
        .andExpect(jsonPath("$.suggestions.Water").value("icons/drink/water"))
        .andExpect(jsonPath("$.categories[0]").value("drink"));
  }

  @Test
  void suggest_returns_exact_match_icon() throws Exception {
    mvc.perform(get("/api/icons/suggest").param("label", "Water"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Water"))
        .andExpect(jsonPath("$.iconPath").value("icons/drink/water"));
  }

  @Test
  void suggest_is_case_insensitive() throws Exception {
    mvc.perform(get("/api/icons/suggest").param("label", "water"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("water"))
        .andExpect(jsonPath("$.iconPath").value("icons/drink/water"));
  }

  @Test
  void suggest_falls_back_to_contains_match() throws Exception {
    mvc.perform(get("/api/icons/suggest").param("label", "I want orange juice please"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.iconPath").value("icons/drink/juice"));
  }

  @Test
  void suggest_returns_blank_icon_for_empty_input() throws Exception {
    mvc.perform(get("/api/icons/suggest").param("label", "   "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value(""))
        .andExpect(jsonPath("$.iconPath").value(""));
  }
}

