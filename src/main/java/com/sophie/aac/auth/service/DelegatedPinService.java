package com.sophie.aac.auth.service;

import com.sophie.aac.auth.domain.DelegatedPinEntity;
import com.sophie.aac.auth.repository.DelegatedPinRepository;
import com.sophie.aac.auth.repository.UserAccountProfileRepository;
import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.common.web.BadRequestException;
import com.sophie.aac.common.web.ForbiddenException;
import com.sophie.aac.common.web.NotFoundException;
import com.sophie.aac.common.web.UnauthorizedException;
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
  private final AuthContext authContext;

  public DelegatedPinService(DelegatedPinRepository delegatedPinRepo,
      UserAccountProfileRepository userAccountProfileRepo,
      PasswordEncoder encoder,
      AuthContext authContext) {
    this.delegatedPinRepo = delegatedPinRepo;
    this.userAccountProfileRepo = userAccountProfileRepo;
    this.encoder = encoder;
    this.authContext = authContext;
  }

  @Transactional
  public DelegatedPinEntity create(String label, String pin, UUID profileId) {
    UUID userId = authContext.currentUserId();
    if (userId == null) {
      throw new UnauthorizedException("Sign in to create a PIN");
    }

    if (!userAccountProfileRepo.findByUserId(userId).stream()
        .anyMatch(up -> up.getProfileId().equals(profileId))) {
      throw new ForbiddenException("No access to this profile");
    }

    if (pin == null || pin.length() < 4) {
      throw new BadRequestException("PIN must be at least 4 digits");
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
    UUID userId = authContext.currentUserId();
    if (userId == null) return List.of();
    return delegatedPinRepo.findByCreatedByUserId(userId);
  }

  @Transactional
  public void revoke(UUID pinId) {
    UUID userId = authContext.currentUserId();
    if (userId == null) {
      throw new UnauthorizedException("Sign in to revoke a PIN");
    }
    var pin = delegatedPinRepo.findById(pinId).orElseThrow(() -> new NotFoundException("PIN not found"));
    if (!pin.getCreatedByUserId().equals(userId)) {
      throw new ForbiddenException("Cannot revoke PIN created by another user");
    }
    pin.setActive(false);
    delegatedPinRepo.save(pin);
  }
}
