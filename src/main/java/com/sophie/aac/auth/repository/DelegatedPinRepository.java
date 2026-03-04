package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.DelegatedPinEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DelegatedPinRepository extends JpaRepository<DelegatedPinEntity, UUID> {
  List<DelegatedPinEntity> findByCreatedByUserId(UUID createdByUserId);
  Optional<DelegatedPinEntity> findByIdAndIsActiveTrue(UUID id);
}
