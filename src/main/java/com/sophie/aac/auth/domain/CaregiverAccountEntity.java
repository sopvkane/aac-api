package com.sophie.aac.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "caregiver_account")
public class CaregiverAccountEntity {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16, unique = true)
  private Role role;

  @Column(nullable = false, length = 100)
  private String pinHash;

  @Column(nullable = false)
  private boolean isActive;

  @Column(nullable = false)
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }

  public String getPinHash() { return pinHash; }
  public void setPinHash(String pinHash) { this.pinHash = pinHash; }

  public boolean isActive() { return isActive; }
  public void setActive(boolean active) { isActive = active; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}