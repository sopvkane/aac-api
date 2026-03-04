package com.sophie.aac.auth.bootstrap;

import com.sophie.aac.auth.domain.JoiningCodeEntity;
import com.sophie.aac.auth.domain.UserAccountEntity;
import com.sophie.aac.auth.domain.UserAccountProfileEntity;
import com.sophie.aac.auth.repository.JoiningCodeRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.repository.UserAccountRepository;
import com.sophie.aac.profile.service.CaregiverProfileService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserAccountSeeder {

  private static final UUID DEMO_CLINICIAN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
  private static final UUID DEMO_PARENT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");

  private final UserAccountRepository userRepo;
  private final UserAccountProfileRepository userProfileRepo;
  private final JoiningCodeRepository joiningCodeRepo;
  private final PasswordEncoder encoder;

  @Value("${DEMO_CLINICIAN_PASSWORD:Demo123!}") private String clinicianPassword;
  @Value("${DEMO_PARENT_PASSWORD:Demo123!}") private String parentPassword;

  public UserAccountSeeder(UserAccountRepository userRepo, UserAccountProfileRepository userProfileRepo,
      JoiningCodeRepository joiningCodeRepo, PasswordEncoder encoder) {
    this.userRepo = userRepo;
    this.userProfileRepo = userProfileRepo;
    this.joiningCodeRepo = joiningCodeRepo;
    this.encoder = encoder;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void seed() {
    seedUser(DEMO_CLINICIAN_ID, "clinician@demo.local", clinicianPassword, "Demo Clinician", "CLINICIAN");
    seedUser(DEMO_PARENT_ID, "parent@demo.local", parentPassword, "Demo Parent", "PARENT_CARER");
    seedJoiningCode();
  }

  private void seedUser(UUID id, String email, String password, String displayName, String role) {
    userRepo.findById(id).ifPresentOrElse(
        existing -> {
          existing.setPasswordHash(encoder.encode(password));
          existing.setActive(true);
          userRepo.save(existing);
        },
        () -> {
          UserAccountEntity u = new UserAccountEntity();
          u.setId(id);
          u.setEmail(email);
          u.setPasswordHash(encoder.encode(password));
          u.setDisplayName(displayName);
          u.setRole(role);
          u.setActive(true);
          u.setCreatedAt(Instant.now());
          userRepo.save(u);

          var link = new UserAccountProfileEntity();
          link.setUserId(u.getId());
          link.setProfileId(CaregiverProfileService.DEFAULT_ID);
          userProfileRepo.save(link);
        }
    );
  }

  private void seedJoiningCode() {
    if (joiningCodeRepo.findByCodeIgnoreCase("DEMO2024").isPresent()) return;

    var jc = new JoiningCodeEntity();
    jc.setId(UUID.randomUUID());
    jc.setCode("DEMO2024");
    jc.setCreatedByUserId(DEMO_CLINICIAN_ID);
    jc.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(365)));
    jc.setMaxUses(100);
    jc.setUsedCount(0);
    jc.setCreatedAt(Instant.now());
    joiningCodeRepo.save(jc);
  }
}
