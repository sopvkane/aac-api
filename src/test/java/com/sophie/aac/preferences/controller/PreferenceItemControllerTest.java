package com.sophie.aac.preferences.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.service.PreferenceItemService;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PreferenceItemControllerTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private PreferenceItemService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = mock(PreferenceItemService.class);
    mvc = MockMvcBuilders.standaloneSetup(new PreferenceItemController(service)).build();
  }

  @Test
  void list_returns_items_from_service() throws Exception {
    PreferenceItemEntity e = new PreferenceItemEntity();
    e.setId(UUID.randomUUID());
    e.setKind("EMOTION");
    e.setLabel("Happy");
    e.setCategory("positive");
    e.setScope("USER");
    e.setPriority(10);
    when(service.listByKind("emotion")).thenReturn(List.of(e));

    mvc.perform(get("/api/carer/preferences").param("kind", "emotion"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Happy"))
        .andExpect(jsonPath("$[0].kind").value("EMOTION"));

    verify(service).listByKind("emotion");
  }

  @Test
  void create_returns_created_item() throws Exception {
    PreferenceItemRequest req = new PreferenceItemRequest("emotion", "Sad", null, null, null, "user", 5);
    PreferenceItemEntity created = new PreferenceItemEntity();
    created.setId(UUID.randomUUID());
    created.setKind("EMOTION");
    created.setLabel("Sad");
    created.setScope("USER");
    created.setPriority(5);
    when(service.create(any(PreferenceItemRequest.class), eq("PARENT"))).thenReturn(created);

    mvc.perform(post("/api/carer/preferences")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Sad"));

    verify(service).create(any(PreferenceItemRequest.class), eq("PARENT"));
  }

  @Test
  void update_calls_service() throws Exception {
    UUID id = UUID.randomUUID();
    PreferenceItemRequest req = new PreferenceItemRequest("emotion", "Joy", null, null, null, "user", 10);
    PreferenceItemEntity updated = new PreferenceItemEntity();
    updated.setId(id);
    updated.setLabel("Joy");
    when(service.update(eq(id), any(PreferenceItemRequest.class))).thenReturn(updated);

    mvc.perform(put("/api/carer/preferences/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk());

    verify(service).update(eq(id), any(PreferenceItemRequest.class));
  }

  @Test
  void delete_calls_service() throws Exception {
    UUID id = UUID.randomUUID();
    mvc.perform(delete("/api/carer/preferences/" + id))
        .andExpect(status().isOk());

    verify(service).delete(id);
  }
}
