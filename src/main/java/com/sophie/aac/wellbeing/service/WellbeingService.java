package com.sophie.aac.wellbeing.service;

import com.sophie.aac.analytics.domain.WellbeingEntryEntity;
import com.sophie.aac.analytics.repository.WellbeingEntryRepository;
import com.sophie.aac.auth.util.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WellbeingService {

    private final WellbeingEntryRepository repo;
    private final AuthContext authContext;

    public WellbeingService(WellbeingEntryRepository repo, AuthContext authContext) {
        this.repo = repo;
        this.authContext = authContext;
    }

    @Transactional
    public void recordMood(int moodScore) {
        UUID profileId = authContext.currentProfileIdOrDefault();
        WellbeingEntryEntity e = new WellbeingEntryEntity();
        e.setId(UUID.randomUUID());
        e.setProfileId(profileId);
        e.setMoodScore(moodScore);
        e.setCreatedAt(Instant.now());
        repo.save(e);
    }

    @Transactional
    public void recordPain(String bodyArea, Integer severity, String notes) {
        UUID profileId = authContext.currentProfileIdOrDefault();
        WellbeingEntryEntity e = new WellbeingEntryEntity();
        e.setId(UUID.randomUUID());
        e.setProfileId(profileId);
        e.setSymptomType("PAIN");
        e.setBodyArea(bodyArea);
        e.setSeverity(severity);
        e.setNotes(notes);
        e.setCreatedAt(Instant.now());
        repo.save(e);
    }
}
