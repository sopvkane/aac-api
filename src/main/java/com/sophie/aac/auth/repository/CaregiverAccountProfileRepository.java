package com.sophie.aac.auth.repository;

import com.sophie.aac.auth.domain.CaregiverAccountProfileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverAccountProfileRepository extends JpaRepository<CaregiverAccountProfileEntity, CaregiverAccountProfileEntity.IdClass> {

  List<CaregiverAccountProfileEntity> findByAccountId(UUID accountId);
}
