package com.sophie.aac.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delegated_pin")
public class DelegatedPinEntity {

  @Id
  private UUID id;

  @Column(nullable = false, length = 100)
  private String pinHash;

  @Column(nullable = false, length = 50)
  private String label;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(nullable = false)
  private boolean isActive;

  @Column(nullable = false)
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getPinHash() { return pinHash; }
  public void setPinHash(String pinHash) { this.pinHash = pinHash; }

  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }

  public UUID getCreatedByUserId() { return createdByUserId; }
  public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

  public UUID getProfileId() { return profileId; }
  public void setProfileId(UUID profileId) { this.profileId = profileId; }

  public boolean isActive() { return isActive; }
  public void setActive(boolean active) { isActive = active; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
