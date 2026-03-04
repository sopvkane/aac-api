package com.sophie.aac.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_session")
public class AuthSessionEntity {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Role role;

  @Column(nullable = false, length = 64, unique = true)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(name = "profile_id")
  private java.util.UUID profileId;

  @Column(name = "user_account_id")
  private UUID userAccountId;

  @Column(name = "delegated_pin_id")
  private UUID delegatedPinId;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }

  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public java.util.UUID getProfileId() { return profileId; }
  public void setProfileId(java.util.UUID profileId) { this.profileId = profileId; }

  public UUID getUserAccountId() { return userAccountId; }
  public void setUserAccountId(UUID userAccountId) { this.userAccountId = userAccountId; }

  public UUID getDelegatedPinId() { return delegatedPinId; }
  public void setDelegatedPinId(UUID delegatedPinId) { this.delegatedPinId = delegatedPinId; }
}