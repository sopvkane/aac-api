package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.repository.DelegatedPinRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.util.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelegatedPinService {

  private final DelegatedPinRepository delegatedPinRepo;
  private final UserAccountProfileRepository userAccountProfileRepo;
  private final PasswordEncoder encoder;

  public DelegatedPinService(DelegatedPinRepository delegatedPinRepo,
      UserAccountProfileRepository userAccountProfileRepo,
      PasswordEncoder encoder) {
    this.delegatedPinRepo = delegatedPinRepo;
    this.userAccountProfileRepo = userAccountProfileRepo;
    this.encoder = encoder;
  }

  @Transactional
  public DelegatedPinEntity create(String label, String pin, UUID profileId) {
    UUID userId = CurrentUser.get();
    if (userId == null) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Sign in to create a PIN");
    }

    if (!userAccountProfileRepo.findByUserId(userId).stream()
        .anyMatch(up -> up.getProfileId().equals(profileId))) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "No access to this profile");
    }

    if (pin == null || pin.length() < 4) {
      throw new IllegalArgumentException("PIN must be at least 4 digits");
    }

    var entity = new DelegatedPinEntity();
    entity.setId(UUID.randomUUID());
    entity.setPinHash(encoder.encode(pin));
    entity.setLabel(label != null && !label.isBlank() ? label.trim() : "Delegate");
    entity.setCreatedByUserId(userId);
    entity.setProfileId(profileId);
    entity.setActive(true);
    entity.setCreatedAt(Instant.now());
    return delegatedPinRepo.save(entity);
  }

  public List<DelegatedPinEntity> listByCurrentUser() {
    UUID userId = CurrentUser.get();
    if (userId == null) return List.of();
    return delegatedPinRepo.findByCreatedByUserId(userId);
  }

  @Transactional
  public void revoke(UUID pinId) {
    UUID userId = CurrentUser.get();
    if (userId == null) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Sign in to revoke a PIN");
    }
    var pin = delegatedPinRepo.findById(pinId).orElseThrow(() ->
        new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "PIN not found"));
    if (!pin.getCreatedByUserId().equals(userId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Cannot revoke PIN created by another user");
    }
    pin.setActive(false);
    delegatedPinRepo.save(pin);
  }
}
