package com.sophie.aac.profile.repository;

import com.sophie.aac.profile.domain.UserProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {}