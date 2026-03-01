package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.CaregiverAccountEntity;
import com.sophie.aac.auth.domain.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverAccountRepository extends JpaRepository<CaregiverAccountEntity, UUID> {
  Optional<CaregiverAccountEntity> findByRole(Role role);
}