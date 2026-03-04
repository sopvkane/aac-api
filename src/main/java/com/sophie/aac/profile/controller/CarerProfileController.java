package com.sophie.aac.profile.controller;

import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.ForbiddenException;
import com.sophie.aac.profile.domain.UserProfileEntity;
import com.sophie.aac.profile.service.CaregiverProfileService;
import com.sophie.aac.profile.service.ProfileSetupService;
import com.sophie.aac.profile.web.CreateProfileRequest;
import com.sophie.aac.profile.web.UpdateUserProfileRequest;
import com.sophie.aac.profile.web.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carer")
public class CarerProfileController {

  private final CaregiverProfileService service;
  private final ProfileSetupService profileSetupService;
  private final AuthContext authContext;

  public CarerProfileController(
      CaregiverProfileService service,
      ProfileSetupService profileSetupService,
      AuthContext authContext
  ) {
    this.service = service;
    this.profileSetupService = profileSetupService;
    this.authContext = authContext;
  }

  @Operation(summary = "Create profile", description = "Create a new communicator profile linked to the current account. Used for the 'set up' / registration flow when adding a new communicator. Requires auth.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Profile created"),
      @ApiResponse(responseCode = "401", description = "Not signed in")
  })
  @PostMapping("/profiles")
  public ResponseEntity<UserProfileResponse> createProfile(@RequestBody @Valid CreateProfileRequest req) {
    UserProfileEntity created = profileSetupService.createProfile(req.displayName(), req.wakeName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Location", "/api/carer/profile")
        .body(toResponse(created));
  }

  @GetMapping("/profile")
  public UserProfileResponse get() {
    return toResponse(service.get());
  }

  @PutMapping("/profile")
  public UserProfileResponse update(@RequestBody @Valid UpdateUserProfileRequest req) {
    Role role = authContext.currentRole();
    if (role != Role.PARENT && role != Role.CLINICIAN) {
      throw new ForbiddenException("Only parent or clinician can update profile");
    }
    return toResponse(service.update(req));
  }

  private static UserProfileResponse toResponse(UserProfileEntity p) {
    return new UserProfileResponse(
        p.getId(),
        p.getDisplayName(),
        p.getWakeName(),
        p.isDetailsDefault(),
        p.isVoiceDefault(),
        p.isAiEnabled(),
        p.isMemoryEnabled(),
        p.isAnalyticsEnabled(),
        p.getDefaultLocation(),
        p.isAllowHome(),
        p.isAllowSchool(),
        p.isAllowWork(),
        p.isAllowOther(),
        p.getMaxOptions(),
        p.getPreferredIconSize(),
        p.getFavFood(),
        p.getFavDrink(),
        p.getFavShow(),
        p.getFavTopic(),
        p.getAboutUser(),
        p.getSchoolDays(),
        p.getLunchTime(),
        p.getDinnerTime(),
        p.getBedTime(),
        p.getFamilyNotes(),
        p.getClassmates(),
        p.getTeachers(),
        p.getSchoolActivities(),
        p.getUpdatedAt()
    );
  }
}
