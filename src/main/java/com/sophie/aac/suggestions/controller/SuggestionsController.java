package com.sophie.aac.suggestions.controller;

import com.sophie.aac.suggestions.service.SuggestionsService;
import com.sophie.aac.suggestions.web.SuggestionsRequest;
import com.sophie.aac.suggestions.web.SuggestionsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionsController {

    private final SuggestionsService suggestionsService;

    public SuggestionsController(SuggestionsService suggestionsService) {
        this.suggestionsService = suggestionsService;
    }

    @PostMapping
    public ResponseEntity<SuggestionsResponse> suggest(@RequestBody SuggestionsRequest request) {
        String prefix = request.prefix() == null ? "" : request.prefix().trim();
        var timeBucket = request.timeBucket();
        var locationCategory = request.locationCategory();

        int limit = 3;

        var suggestions = suggestionsService.suggest(prefix, timeBucket, locationCategory, limit);

        var response = new SuggestionsResponse(
            suggestions,
            new SuggestionsResponse.Meta(prefix == null ? "" : prefix.trim(), timeBucket, locationCategory, limit)
        );

        return ResponseEntity.ok(response);
    }
}