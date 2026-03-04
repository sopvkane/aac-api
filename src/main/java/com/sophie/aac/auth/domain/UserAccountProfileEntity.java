package com.sophie.aac.auth.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_account_profile")
@IdClass(UserAccountProfileEntity.IdClass.class)
public class UserAccountProfileEntity {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Id
  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  public static class IdClass implements Serializable {
    public UUID userId;
    public UUID profileId;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IdClass that = (IdClass) o;
      return java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(profileId, that.profileId);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(userId, profileId);
    }
  }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public UUID getProfileId() { return profileId; }
  public void setProfileId(UUID profileId) { this.profileId = profileId; }
}
