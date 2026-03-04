package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.JoiningCodeEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoiningCodeRepository extends JpaRepository<JoiningCodeEntity, UUID> {
  Optional<JoiningCodeEntity> findByCodeIgnoreCase(String code);
}
