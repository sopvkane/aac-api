package com.sophie.aac.phrases.controller;

import com.sophie.aac.phrases.service.PhraseService;
import com.sophie.aac.phrases.web.CreatePhraseRequest;
import com.sophie.aac.phrases.web.PhraseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/phrases")
public class PhraseController {

  private final PhraseService service;

  public PhraseController(PhraseService service) {
    this.service = service;
  }

  @GetMapping
  public List<PhraseResponse> list(
      @RequestParam Optional<String> q,
      @RequestParam Optional<String> category
  ) {
    return service.list(q, category).stream()
        .map(PhraseResponse::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<PhraseResponse> create(@Valid @RequestBody CreatePhraseRequest req) {
    var saved = service.create(req.text(), req.category());
    return ResponseEntity
        .created(URI.create("/api/phrases/" + saved.getId()))
        .body(PhraseResponse.from(saved));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
