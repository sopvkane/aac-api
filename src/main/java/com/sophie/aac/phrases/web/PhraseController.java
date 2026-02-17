package com.sophie.aac.phrases.web;

import com.sophie.aac.phrases.service.PhraseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/phrases")
public class PhraseController {

  private final PhraseService service;

  public PhraseController(PhraseService service) {
    this.service = service;
  }

  @GetMapping
  public List<PhraseResponse> list() {
    return service.list().stream()
        .map(p -> new PhraseResponse(p.getId(), p.getText(), p.getCreatedAt()))
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PhraseResponse create(@Valid @RequestBody CreatePhraseRequest req) {
    var saved = service.create(req.text());
    return new PhraseResponse(saved.getId(), saved.getText(), saved.getCreatedAt());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
