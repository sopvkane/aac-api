package com.sophie.aac.phrases.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phrases")
public class PhraseEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "text", nullable = false, length = 280)
  private String text;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PhraseEntity() {
    // JPA
  }

  public PhraseEntity(UUID id, String text, Instant createdAt) {
    this.id = id;
    this.text = text;
    this.createdAt = createdAt;
  }

  @PrePersist
  void onCreate() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setText(String text) {
    this.text = text;
  }
}
