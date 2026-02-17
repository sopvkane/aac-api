package com.sophie.aac.phrases.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phrases")
public class Phrase {

  @Id
  @GeneratedValue
  private UUID id;

  @NotBlank
  @Column(nullable = false)
  private String text;

  private String category;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }

  public UUID getId() { return id; }
  public String getText() { return text; }
  public String getCategory() { return category; }
  public Instant getCreatedAt() { return createdAt; }

  public void setId(UUID id) { this.id = id; }
  public void setText(String text) { this.text = text; }
  public void setCategory(String category) { this.category = category; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
