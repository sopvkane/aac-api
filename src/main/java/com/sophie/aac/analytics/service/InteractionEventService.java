package com.sophie.aac.analytics.service;

import com.sophie.aac.analytics.domain.InteractionEventEntity;
import com.sophie.aac.analytics.repository.InteractionEventRepository;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.suggestions.domain.LocationCategory;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionEventService {

  private final InteractionEventRepository repo;
  private final AuthContext authContext;

  public InteractionEventService(InteractionEventRepository repo, AuthContext authContext) {
    this.repo = repo;
    this.authContext = authContext;
  }

  @Transactional
  public void record(String eventType, LocationCategory location, String promptType, String selectedText) {
    UUID profileId = authContext.currentProfileIdOrDefault();
    InteractionEventEntity e = new InteractionEventEntity();
    e.setId(UUID.randomUUID());
    e.setProfileId(profileId);
    e.setEventType(eventType != null && !eventType.isBlank() ? eventType : "OPTION_SELECTED");
    e.setLocation(location != null ? location : LocationCategory.HOME);
    e.setPromptType(promptType);
    e.setSelectedText(selectedText != null && selectedText.length() > 280 ? selectedText.substring(0, 280) : selectedText);
    e.setCreatedAt(Instant.now());
    repo.save(e);
  }
}
