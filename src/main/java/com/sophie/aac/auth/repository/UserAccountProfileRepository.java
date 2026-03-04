package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.UserAccountProfileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountProfileRepository extends JpaRepository<UserAccountProfileEntity, UserAccountProfileEntity.IdClass> {
  List<UserAccountProfileEntity> findByUserId(UUID userId);
}
