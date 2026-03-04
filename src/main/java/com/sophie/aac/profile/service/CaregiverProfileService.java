package com.sophie.aac.profile.service;

import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.repository.UserProfileRepository;
import com.sophie.aac.profile.web.UpdateUserProfileRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaregiverProfileService {

  public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final UserProfileRepository repo;
  private final AuthContext authContext;

  public CaregiverProfileService(UserProfileRepository repo, AuthContext authContext) {
    this.repo = repo;
    this.authContext = authContext;
  }

  /**
   * Returns the active profile. Requires authentication – throws when guest (unauthenticated).
   * Use getOrDefault() for callers that support guest mode.
   */
  public UserProfileEntity get() {
    UUID profileId = authContext.currentProfileId();
    if (profileId == null) {
      throw new IllegalStateException("No active profile. Sign in to access profile data.");
    }
    return repo.findById(profileId)
        .orElseThrow(() -> new IllegalStateException("Profile not found: " + profileId));
  }

  @Transactional
  public UserProfileEntity update(UpdateUserProfileRequest r) {
    UserProfileEntity p = get();

    p.setDisplayName(r.displayName());
    p.setWakeName(r.wakeName());
    p.setDetailsDefault(r.detailsDefault());
    p.setVoiceDefault(r.voiceDefault());

    p.setAiEnabled(r.aiEnabled());
    p.setMemoryEnabled(r.memoryEnabled());
    p.setAnalyticsEnabled(r.analyticsEnabled());

    p.setDefaultLocation(r.defaultLocation());
    p.setAllowHome(r.allowHome());
    p.setAllowSchool(r.allowSchool());
    p.setAllowWork(r.allowWork());
    p.setAllowOther(r.allowOther());
    p.setMaxOptions(r.maxOptions());

    if (r.preferredIconSize() != null && !r.preferredIconSize().isBlank()) {
      String size = r.preferredIconSize().trim().toLowerCase();
      if (size.equals("small") || size.equals("medium") || size.equals("large")) {
        p.setPreferredIconSize(size);
      }
    }

    p.setFavFood(r.favFood());
    p.setFavDrink(r.favDrink());
    p.setFavShow(r.favShow());
    p.setFavTopic(r.favTopic());

    p.setAboutUser(r.aboutUser());
    p.setSchoolDays(r.schoolDays());
    p.setLunchTime(r.lunchTime());
    p.setDinnerTime(r.dinnerTime());
    p.setBedTime(r.bedTime());

    p.setFamilyNotes(r.familyNotes());
    p.setClassmates(r.classmates());
    p.setTeachers(r.teachers());
    p.setSchoolActivities(r.schoolActivities());

    p.setUpdatedAt(Instant.now());
    return repo.save(p);
  }
}
