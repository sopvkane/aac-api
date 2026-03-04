package com.sophie.aac.profile.service;

import com.sophie.aac.auth.domain.CaregiverAccountProfileEntity;
import com.sophie.aac.auth.domain.UserAccountProfileEntity;
import com.sophie.aac.auth.repository.CaregiverAccountProfileRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.UnauthorizedException;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.suggestions.domain.LocationCategory;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates new communicator profiles and links them to the current account.
 * Supports both full user accounts and legacy caregiver accounts.
 */
@Service
public class ProfileSetupService {

  private final UserProfileRepository profileRepo;
  private final CaregiverAccountRepository accountRepo;
  private final CaregiverAccountProfileRepository accountProfileRepo;
  private final UserAccountProfileRepository userAccountProfileRepo;
  private final AuthContext authContext;

  public ProfileSetupService(
      UserProfileRepository profileRepo,
      CaregiverAccountRepository accountRepo,
      CaregiverAccountProfileRepository accountProfileRepo,
      UserAccountProfileRepository userAccountProfileRepo,
      AuthContext authContext) {
    this.profileRepo = profileRepo;
    this.accountRepo = accountRepo;
    this.accountProfileRepo = accountProfileRepo;
    this.userAccountProfileRepo = userAccountProfileRepo;
    this.authContext = authContext;
  }

  @Transactional
  public UserProfileEntity createProfile(String displayName, String wakeName) {
    var role = authContext.currentRole();
    if (role == null) {
      throw new UnauthorizedException("Sign in to create a profile");
    }

    UUID userId = authContext.currentUserId();
    if (userId != null) {
      return createProfileForUser(userId, displayName, wakeName);
    }

    var account = accountRepo.findByRole(role)
        .orElseThrow(() -> new IllegalStateException("Account not found for role: " + role));

    var profile = new UserProfileEntity();
    profile.setId(UUID.randomUUID());
    profile.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : "New User");
    profile.setWakeName(wakeName != null && !wakeName.isBlank() ? wakeName.trim() : "Hey");
    profile.setDetailsDefault(true);
    profile.setVoiceDefault(false);
    profile.setAiEnabled(true);
    profile.setMemoryEnabled(true);
    profile.setAnalyticsEnabled(false);
    profile.setDefaultLocation(LocationCategory.HOME);
    profile.setAllowHome(true);
    profile.setAllowSchool(true);
    profile.setAllowWork(false);
    profile.setAllowOther(true);
    profile.setMaxOptions(3);
    profile.setPreferredIconSize("large");
    profile.setUpdatedAt(Instant.now());

    profileRepo.save(profile);

    var link = new CaregiverAccountProfileEntity();
    link.setAccountId(account.getId());
    link.setProfileId(profile.getId());
    accountProfileRepo.save(link);

    return profile;
  }

  private UserProfileEntity createProfileForUser(UUID userId, String displayName, String wakeName) {
    var profile = new UserProfileEntity();
    profile.setId(UUID.randomUUID());
    profile.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : "New User");
    profile.setWakeName(wakeName != null && !wakeName.isBlank() ? wakeName.trim() : "Hey");
    profile.setDetailsDefault(true);
    profile.setVoiceDefault(false);
    profile.setAiEnabled(true);
    profile.setMemoryEnabled(true);
    profile.setAnalyticsEnabled(false);
    profile.setDefaultLocation(LocationCategory.HOME);
    profile.setAllowHome(true);
    profile.setAllowSchool(true);
    profile.setAllowWork(false);
    profile.setAllowOther(true);
    profile.setMaxOptions(3);
    profile.setPreferredIconSize("large");
    profile.setUpdatedAt(Instant.now());

    profileRepo.save(profile);

    var link = new UserAccountProfileEntity();
    link.setUserId(userId);
    link.setProfileId(profile.getId());
    userAccountProfileRepo.save(link);

    return profile;
  }
}
