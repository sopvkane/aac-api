package com.sophie.aac.preferences.repository;

import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PreferenceItemRepository extends JpaRepository<PreferenceItemEntity, UUID> {

    List<PreferenceItemEntity> findByProfileIdAndKindOrderByPriorityDescLabelAsc(UUID profileId, String kind);

    List<PreferenceItemEntity> findByProfileIdAndKindInAndScopeInOrderByPriorityDescLabelAsc(
        UUID profileId, List<String> kinds, List<String> scopes);
}

