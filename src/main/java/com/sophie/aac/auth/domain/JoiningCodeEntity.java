package com.sophie.aac.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "joining_code")
public class JoiningCodeEntity {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 16)
  private String code;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(name = "max_uses")
  private Integer maxUses;

  @Column(nullable = false)
  private int usedCount;

  @Column(nullable = false)
  private Instant createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public UUID getCreatedByUserId() { return createdByUserId; }
  public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

  public Integer getMaxUses() { return maxUses; }
  public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }

  public int getUsedCount() { return usedCount; }
  public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public boolean isValid() {
    if (expiresAt.isBefore(Instant.now())) return false;
    if (maxUses != null && usedCount >= maxUses) return false;
    return true;
  }
}
