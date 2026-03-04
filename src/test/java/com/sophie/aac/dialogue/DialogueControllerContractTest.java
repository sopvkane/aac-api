package com.sophie.aac.dialogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophie.aac.dialogue.controller.DialogueController;
import com.sophie.aac.dialogue.service.DialogueService;
import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DialogueControllerContractTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private DialogueService dialogueService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    dialogueService = mock(DialogueService.class);
    mvc = MockMvcBuilders.standaloneSetup(new DialogueController(dialogueService)).build();
  }

  @Test
  void replies_returns_expected_contract_shape() throws Exception {
    DialogueResponse payload = new DialogueResponse(
        "DRINK",
        List.of(
            new DialogueResponse.Reply("r1", "Water", "I would like water, please.", null, "DRINK"),
            new DialogueResponse.Reply("r2", "Juice", "I would like juice, please.", null, "DRINK"),
            new DialogueResponse.Reply("r3", "Help", "Can you help me, please?", null, "GENERIC")
        ),
        List.of(new DialogueResponse.OptionGroup("drinks", "Drinks", List.of("Water", "Juice"))),
        new DialogueResponse.Memory("drink", "would you like a drink", List.of()),
        Map.of("dialogueMode", "SEMANTIC", "llmUsed", false)
    );

    when(dialogueService.generateReplies(org.mockito.ArgumentMatchers.any(DialogueRequest.class))).thenReturn(payload);

    mvc.perform(post("/api/dialogue/replies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new DialogueRequest(
                "Sophie", "Would you like a drink?", Map.of("location", "HOME"), null
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.intent").value("DRINK"))
        .andExpect(jsonPath("$.topReplies[0].id").value("r1"))
        .andExpect(jsonPath("$.topReplies[0].label").value("Water"))
        .andExpect(jsonPath("$.topReplies[0].requiresPersonSelection").value(true))
        .andExpect(jsonPath("$.optionGroups[0].id").value("drinks"))
        .andExpect(jsonPath("$.memory.lastIntent").value("drink"))
        .andExpect(jsonPath("$.debug.dialogueMode").value("SEMANTIC"));
  }
}
