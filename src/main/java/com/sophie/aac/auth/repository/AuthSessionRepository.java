package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.AuthSessionEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {
  Optional<AuthSessionEntity> findByTokenHash(String tokenHash);
  void deleteByTokenHash(String tokenHash);
  void deleteByExpiresAtBefore(Instant now);
}