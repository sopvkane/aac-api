package com.sophie.aac.analytics.controller;

import com.sophie.aac.analytics.service.InteractionEventService;
import com.sophie.aac.suggestions.domain.LocationCategory;
import java.util.Locale;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions")
public class InteractionEventController {

  private final InteractionEventService service;

  public InteractionEventController(InteractionEventService service) {
    this.service = service;
  }

  public record RecordRequest(String eventType, String location, String promptType, String selectedText) {}

  @PostMapping
  public void record(@RequestBody RecordRequest req) {
    LocationCategory loc = toLocationCategory(req.location());
    service.record(req.eventType(), loc, req.promptType(), req.selectedText());
  }

  private static LocationCategory toLocationCategory(String loc) {
    if (loc == null || loc.isBlank()) return LocationCategory.HOME;
    String u = loc.trim().toUpperCase(Locale.ROOT);
    return switch (u) {
      case "SCHOOL" -> LocationCategory.SCHOOL;
      case "WORK" -> LocationCategory.WORK;
      case "OTHER", "OUT" -> LocationCategory.OTHER;
      case "BUS" -> LocationCategory.HOME; // Bus = with family
      default -> LocationCategory.HOME;
    };
  }
}
