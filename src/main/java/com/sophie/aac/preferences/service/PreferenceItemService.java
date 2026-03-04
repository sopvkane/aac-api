package com.sophie.aac.preferences.service;

import com.sophie.aac.auth.util.CurrentProfile;
import com.sophie.aac.preferences.domain.PreferenceItemEntity;
import com.sophie.aac.preferences.repository.PreferenceItemRepository;
import com.sophie.aac.preferences.web.PreferenceItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PreferenceItemService {

    private final PreferenceItemRepository repo;

    public PreferenceItemService(PreferenceItemRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns preferences for the current profile. When unauthenticated (guest), returns empty list
     * so guests see no saved preference data.
     */
    public List<PreferenceItemEntity> listByKind(String kind) {
        UUID profileId = CurrentProfile.get();
        if (profileId == null) {
            return List.of();
        }
        return repo.findByProfileIdAndKindOrderByPriorityDescLabelAsc(profileId, kind.toUpperCase());
    }

    public PreferenceItemEntity findById(UUID id) {
        return repo.findById(id).orElse(null);
    }

    /**
     * Returns "who to ask" people for the Speak tab, filtered by location.
     * HOME → family only; SCHOOL → teachers only; BUS → bus staff only.
     * Returns empty when unauthenticated (guest).
     */
    public List<PreferenceItemEntity> listWhoToAskByLocation(String location) {
        UUID profileId = CurrentProfile.get();
        if (profileId == null) {
            return List.of();
        }
        if (location == null || location.isBlank()) location = "HOME";
        String loc = location.toUpperCase();
        if ("BUS".equals(loc)) {
            return repo.findByProfileIdAndKindInAndScopeInOrderByPriorityDescLabelAsc(
                profileId, List.of("BUS_STAFF"), List.of("SCHOOL", "BOTH"));
        }
        if ("SCHOOL".equals(loc)) {
            return repo.findByProfileIdAndKindInAndScopeInOrderByPriorityDescLabelAsc(
                profileId, List.of("TEACHER"), List.of("SCHOOL", "BOTH"));
        }
        return repo.findByProfileIdAndKindInAndScopeInOrderByPriorityDescLabelAsc(
            profileId, List.of("FAMILY_MEMBER"), List.of("HOME", "BOTH"));
    }

    @Transactional
    public PreferenceItemEntity create(PreferenceItemRequest req, String createdByRole) {
        UUID profileId = CurrentProfile.require();
        PreferenceItemEntity e = new PreferenceItemEntity();
        e.setId(UUID.randomUUID());
        e.setProfileId(profileId);
        e.setKind(req.kind().toUpperCase());
        e.setLabel(req.label().trim());
        e.setCategory(trimOrNull(req.category()));
        e.setTags(trimOrNull(req.tags()));
        e.setImageUrl(trimOrNull(req.imageUrl()));
        e.setScope(req.scope().toUpperCase());
        e.setPriority(req.priority() != null ? req.priority() : 0);
        e.setCreatedByRole(createdByRole);
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return repo.save(e);
    }

    @Transactional
    public PreferenceItemEntity update(UUID id, PreferenceItemRequest req) {
        UUID profileId = CurrentProfile.require();
        PreferenceItemEntity e = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Preference item not found"));
        if (!e.getProfileId().equals(profileId)) {
            throw new IllegalArgumentException("Preference item not found");
        }

        e.setKind(req.kind().toUpperCase());
        e.setLabel(req.label().trim());
        e.setCategory(trimOrNull(req.category()));
        e.setTags(trimOrNull(req.tags()));
        e.setImageUrl(trimOrNull(req.imageUrl()));
        e.setScope(req.scope().toUpperCase());
        if (req.priority() != null) {
            e.setPriority(req.priority());
        }
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    @Transactional
    public void delete(UUID id) {
        UUID profileId = CurrentProfile.require();
        PreferenceItemEntity e = repo.findById(id).orElse(null);
        if (e != null && !e.getProfileId().equals(profileId)) {
            throw new IllegalArgumentException("Preference item not found");
        }
        repo.deleteById(id);
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

