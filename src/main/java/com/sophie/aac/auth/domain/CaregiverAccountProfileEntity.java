package com.sophie.aac.auth.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "caregiver_account_profile")
@IdClass(CaregiverAccountProfileEntity.IdClass.class)
public class CaregiverAccountProfileEntity {

  @Id
  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Id
  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  public static class IdClass implements Serializable {
    public UUID accountId;
    public UUID profileId;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IdClass that = (IdClass) o;
      return java.util.Objects.equals(accountId, that.accountId) && java.util.Objects.equals(profileId, that.profileId);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(accountId, profileId);
    }
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getProfileId() {
    return profileId;
  }

  public void setProfileId(UUID profileId) {
    this.profileId = profileId;
  }
}
