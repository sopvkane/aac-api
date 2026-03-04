package com.sophie.aac.preferences.controller;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.util.AuthContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.service.PreferenceItemService;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
import com.sophie.aac.common.web.ApiExceptionHandler;
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
  private AuthContext authContext;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = mock(PreferenceItemService.class);
    authContext = mock(AuthContext.class);
    when(authContext.currentRole()).thenReturn(Role.PARENT);
    mvc = MockMvcBuilders.standaloneSetup(new PreferenceItemController(service, authContext))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
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
  void create_returns_403_when_unauthenticated() throws Exception {
    when(authContext.currentRole()).thenReturn(null);
    PreferenceItemRequest req = new PreferenceItemRequest("food", "Toast", null, null, null, "home", 5);

    mvc.perform(post("/api/carer/preferences")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isForbidden());

    verify(service, never()).create(any(), any());
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

  @Test
  void whoToAsk_returns_correct_people_per_location() throws Exception {
    PreferenceItemEntity mum = new PreferenceItemEntity();
    mum.setId(UUID.randomUUID());
    mum.setKind("FAMILY_MEMBER");
    mum.setLabel("Mum");
    mum.setScope("HOME");
    mum.setPriority(10);
    when(service.listWhoToAskByLocation("HOME")).thenReturn(List.of(mum));

    PreferenceItemEntity mrsPatel = new PreferenceItemEntity();
    mrsPatel.setId(UUID.randomUUID());
    mrsPatel.setKind("TEACHER");
    mrsPatel.setLabel("Mrs Patel");
    mrsPatel.setScope("SCHOOL");
    mrsPatel.setPriority(10);
    when(service.listWhoToAskByLocation("SCHOOL")).thenReturn(List.of(mrsPatel));

    PreferenceItemEntity dave = new PreferenceItemEntity();
    dave.setId(UUID.randomUUID());
    dave.setKind("BUS_STAFF");
    dave.setLabel("Dave (driver)");
    dave.setScope("SCHOOL");
    dave.setPriority(10);
    when(service.listWhoToAskByLocation("BUS")).thenReturn(List.of(dave));

    mvc.perform(get("/api/carer/preferences/who-to-ask").param("location", "HOME"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Suggested-Icon-Size", "large"))
        .andExpect(jsonPath("$[0].kind").value("FAMILY_MEMBER"))
        .andExpect(jsonPath("$[0].label").value("Mum"));

    mvc.perform(get("/api/carer/preferences/who-to-ask").param("location", "SCHOOL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].kind").value("TEACHER"))
        .andExpect(jsonPath("$[0].label").value("Mrs Patel"));

    mvc.perform(get("/api/carer/preferences/who-to-ask").param("location", "BUS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].kind").value("BUS_STAFF"))
        .andExpect(jsonPath("$[0].label").value("Dave (driver)"));

    verify(service).listWhoToAskByLocation("HOME");
    verify(service).listWhoToAskByLocation("SCHOOL");
    verify(service).listWhoToAskByLocation("BUS");
  }

  @Test
  void whoToAsk_defaults_to_HOME_when_no_location() throws Exception {
    when(service.listWhoToAskByLocation("HOME")).thenReturn(List.of());

    mvc.perform(get("/api/carer/preferences/who-to-ask"))
        .andExpect(status().isOk());

    verify(service).listWhoToAskByLocation("HOME");
  }

  @Test
  void list_returns_403_for_carer_requesting_school_kind() throws Exception {
    when(authContext.currentRole()).thenReturn(Role.CARER);

    mvc.perform(get("/api/carer/preferences").param("kind", "TEACHER"))
        .andExpect(status().isForbidden());

    verify(service, never()).listByKind(any());
  }

  @Test
  void list_returns_200_for_carer_requesting_food() throws Exception {
    when(authContext.currentRole()).thenReturn(Role.CARER);
    when(service.listByKind("FOOD")).thenReturn(List.of());

    mvc.perform(get("/api/carer/preferences").param("kind", "FOOD"))
        .andExpect(status().isOk());

    verify(service).listByKind("FOOD");
  }

  @Test
  void list_returns_403_when_unauthenticated() throws Exception {
    when(authContext.currentRole()).thenReturn(null);

    mvc.perform(get("/api/carer/preferences").param("kind", "FOOD"))
        .andExpect(status().isForbidden());

    verify(service, never()).listByKind(any());
  }
}
