package com.sophie.aac.auth.bootstrap;

import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.CaregiverAccountProfileEntity;
import com.sophie.aac.auth.domain.Role;
import com.sophie.aac.auth.repository.CaregiverAccountProfileRepository;
import com.sophie.aac.auth.repository.CaregiverAccountRepository;
import com.sophie.aac.auth.util.AuthContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthAccountSeeder {

  private final CaregiverAccountRepository repo;
  private final CaregiverAccountProfileRepository accountProfileRepo;
  private final PasswordEncoder encoder;

  @Value("${PARENT_PIN:1234}") private String parentPin;
  @Value("${CARER_PIN:2345}") private String carerPin;
  @Value("${CLINICIAN_PIN:3456}") private String clinicianPin;
  @Value("${SCHOOL_ADMIN_PIN:4567}") private String schoolAdminPin;
  @Value("${SCHOOL_TEACHER_PIN:${TEACHER_PIN:5678}}") private String schoolTeacherPin;

  public AuthAccountSeeder(
      CaregiverAccountRepository repo,
      CaregiverAccountProfileRepository accountProfileRepo,
      PasswordEncoder encoder
  ) {
    this.repo = repo;
    this.accountProfileRepo = accountProfileRepo;
    this.encoder = encoder;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void seed() {
    seedRole(Role.PARENT, parentPin);
    seedRole(Role.CARER, carerPin);
    seedRole(Role.CLINICIAN, clinicianPin);
    seedRole(Role.SCHOOL_ADMIN, schoolAdminPin);
    seedRole(Role.SCHOOL_TEACHER, schoolTeacherPin);
  }

  private void seedRole(Role role, String pin) {
    repo.findByRole(role).ifPresentOrElse(
        existing -> {
          // Keep the demo accounts in sync with the configured PINs
          existing.setPinHash(encoder.encode(pin));
          existing.setActive(true);
          CaregiverAccountEntity saved = repo.save(existing);
          ensureDefaultProfileLink(saved.getId());
        },
        () -> {
          CaregiverAccountEntity a = new CaregiverAccountEntity();
          a.setId(UUID.randomUUID());
          a.setRole(role);
          a.setPinHash(encoder.encode(pin));
          a.setActive(true);
          a.setCreatedAt(Instant.now());
          CaregiverAccountEntity saved = repo.save(a);
          ensureDefaultProfileLink(saved.getId());
        }
    );
  }

  private void ensureDefaultProfileLink(UUID accountId) {
    var id = new CaregiverAccountProfileEntity.IdClass();
    id.accountId = accountId;
    id.profileId = AuthContext.DEFAULT_PROFILE_ID;
    if (accountProfileRepo.existsById(id)) return;

    CaregiverAccountProfileEntity link = new CaregiverAccountProfileEntity();
    link.setAccountId(accountId);
    link.setProfileId(AuthContext.DEFAULT_PROFILE_ID);
    accountProfileRepo.save(link);
  }
}
