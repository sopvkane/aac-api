package com.sophie.aac.speech.controller;

import com.sophie.aac.speech.service.SpeechTokenService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/speech")
public class SpeechTokenController {

    private final SpeechTokenService speechTokenService;

    public SpeechTokenController(SpeechTokenService speechTokenService) {
        this.speechTokenService = speechTokenService;
    }

    @GetMapping("/token")
    public ResponseEntity<SpeechTokenService.TokenResponse> token() {
        var body = speechTokenService.getToken();
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(body);
    }
}