package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.AuthSessionEntity;
import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.CaregiverAccountProfileEntity;
import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.domain.UserAccountProfileEntity;
import com.sophie.aac.auth.repository.CaregiverAccountProfileRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.repository.DelegatedPinRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthProfileAccessService {

  private final CaregiverAccountRepository accounts;
  private final CaregiverAccountProfileRepository accountProfiles;
  private final UserAccountProfileRepository userAccountProfiles;
  private final DelegatedPinRepository delegatedPins;

  public AuthProfileAccessService(
      CaregiverAccountRepository accounts,
      CaregiverAccountProfileRepository accountProfiles,
      UserAccountProfileRepository userAccountProfiles,
      DelegatedPinRepository delegatedPins
  ) {
    this.accounts = accounts;
    this.accountProfiles = accountProfiles;
    this.userAccountProfiles = userAccountProfiles;
    this.delegatedPins = delegatedPins;
  }

  List<UUID> getProfileIdsForRole(Role role) {
    return accounts.findByRole(role)
        .map(acc -> accountProfiles.findByAccountId(acc.getId()).stream()
            .map(CaregiverAccountProfileEntity::getProfileId)
            .toList())
        .orElse(List.of());
  }

  List<UUID> getProfileIdsForSession(AuthSessionEntity session) {
    if (session.getUserAccountId() != null) {
      return userAccountProfiles.findByUserId(session.getUserAccountId()).stream()
          .map(UserAccountProfileEntity::getProfileId)
          .toList();
    }
    if (session.getDelegatedPinId() != null) {
      return delegatedPins.findById(session.getDelegatedPinId())
          .filter(DelegatedPinEntity::isActive)
          .map(dp -> List.of(dp.getProfileId()))
          .orElse(List.of());
    }
    return getProfileIdsForRole(session.getRole());
  }

  boolean hasAccessToProfile(AuthSessionEntity session, UUID profileId) {
    return getProfileIdsForSession(session).stream().anyMatch(profileId::equals);
  }

  CaregiverAccountEntity requireActiveCaregiverAccount(Role role) {
    return accounts.findByRole(role)
        .filter(CaregiverAccountEntity::isActive)
        .orElseThrow(() -> new IllegalArgumentException("Invalid role or account inactive"));
  }
}
