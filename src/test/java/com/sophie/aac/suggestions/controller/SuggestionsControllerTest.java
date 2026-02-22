package com.sophie.aac.suggestions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.web.PhraseResponse;
import com.sophie.aac.suggestions.domain.LocationCategory;
import com.sophie.aac.suggestions.domain.TimeBucket;
import com.sophie.aac.suggestions.service.SuggestionsService;
import com.sophie.aac.suggestions.web.SuggestionItem;
import com.sophie.aac.suggestions.web.SuggestionsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SuggestionsControllerTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private SuggestionsService suggestionsService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    suggestionsService = mock(SuggestionsService.class);
    mvc = MockMvcBuilders
        .standaloneSetup(new SuggestionsController(suggestionsService))
        .build();
  }

  @Test
  void suggest_returns_suggestions_and_meta_and_calls_service_with_limit_3() throws Exception {
    UUID id1 = UUID.randomUUID();
    PhraseEntity p1 = new PhraseEntity();
    p1.setId(id1);
    p1.setText("Can you help me?");
    p1.setCategory("general");

    UUID id2 = UUID.randomUUID();
    PhraseEntity p2 = new PhraseEntity();
    p2.setId(id2);
    p2.setText("Can I have water?");
    p2.setCategory("needs");

    UUID id3 = UUID.randomUUID();
    PhraseEntity p3 = new PhraseEntity();
    p3.setId(id3);
    p3.setText("Can we go home?");
    p3.setCategory("travel");

    var items = List.of(
        new SuggestionItem(PhraseResponse.from(p1), 1.0),
        new SuggestionItem(PhraseResponse.from(p2), 0.9),
        new SuggestionItem(PhraseResponse.from(p3), 0.8)
    );

    when(suggestionsService.suggest("can", TimeBucket.MORNING, LocationCategory.HOME, 3))
        .thenReturn(items);

    mvc.perform(post("/api/suggestions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SuggestionsRequest("can", TimeBucket.MORNING, LocationCategory.HOME)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions", hasSize(3)))
        .andExpect(jsonPath("$.suggestions[0].phrase.id").value(id1.toString()))
        .andExpect(jsonPath("$.suggestions[0].phrase.text").value("Can you help me?"))
        .andExpect(jsonPath("$.suggestions[0].phrase.category").value("general"))
        .andExpect(jsonPath("$.suggestions[0].score").value(closeTo(1.0, 0.0001)))
        .andExpect(jsonPath("$.meta.prefix").value("can"))
        .andExpect(jsonPath("$.meta.timeBucket").value("MORNING"))
        .andExpect(jsonPath("$.meta.locationCategory").value("HOME"))
        .andExpect(jsonPath("$.meta.limit").value(3));

    verify(suggestionsService).suggest("can", TimeBucket.MORNING, LocationCategory.HOME, 3);
  }

  @Test
  void suggest_trims_prefix_in_meta_but_passes_raw_prefix_to_service() throws Exception {
    when(suggestionsService.suggest("can", TimeBucket.EVENING, LocationCategory.OTHER, 3))
        .thenReturn(List.of());

    mvc.perform(post("/api/suggestions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SuggestionsRequest("can", TimeBucket.EVENING, LocationCategory.OTHER)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions", hasSize(0)))
        .andExpect(jsonPath("$.meta.prefix").value("can"));

    verify(suggestionsService).suggest("can", TimeBucket.EVENING, LocationCategory.OTHER, 3);
    verify(suggestionsService).suggest("can", TimeBucket.EVENING, LocationCategory.OTHER, 3);
  }
}