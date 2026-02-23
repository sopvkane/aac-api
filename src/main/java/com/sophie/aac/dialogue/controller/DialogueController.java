package com.sophie.aac.dialogue.controller;

import com.sophie.aac.dialogue.service.DialogueService;
import com.sophie.aac.dialogue.web.DialogueRequest;
import com.sophie.aac.dialogue.web.DialogueResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dialogue")
public class DialogueController {

    private final DialogueService dialogueService;

    public DialogueController(DialogueService dialogueService) {
        this.dialogueService = dialogueService;
    }

    @PostMapping("/replies")
    public ResponseEntity<DialogueResponse> replies(@RequestBody DialogueRequest request) {
        return ResponseEntity.ok(dialogueService.generateReplies(request));
    }
}