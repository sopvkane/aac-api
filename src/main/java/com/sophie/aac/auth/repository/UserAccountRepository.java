package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.UserAccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {
  Optional<UserAccountEntity> findByEmailIgnoreCase(String email);
}
