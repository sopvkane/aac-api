package com.sophie.aac.phrases.service;

import com.sophie.aac.auth.util.AuthContext;
import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.domain.PhraseNotFoundException;
import com.sophie.aac.phrases.repository.PhraseRepository;
import com.sophie.aac.phrases.repository.PhraseSpecs;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhraseService {

  private final PhraseRepository repo;
  private final AuthContext authContext;

  public PhraseService(PhraseRepository repo, AuthContext authContext) {
    this.repo = repo;
    this.authContext = authContext;
  }

  @Transactional(readOnly = true)
  public List<PhraseEntity> list(Optional<String> q, Optional<String> category) {
    UUID profileId = authContext.requireCurrentProfileId();
    Specification<PhraseEntity> spec = PhraseSpecs.profileIdEquals(profileId);

    if (q.isPresent() && !q.get().isBlank()) {
      spec = spec.and(PhraseSpecs.textContainsIgnoreCase(q.get()));
    }
    if (category.isPresent() && !category.get().isBlank()) {
      spec = spec.and(PhraseSpecs.categoryEqualsIgnoreCase(category.get()));
    }

    return repo.findAll(spec, Sort.by(Sort.Direction.ASC, "text"));
  }

  @Transactional
  public PhraseEntity create(String text, String category, String iconUrl) {
    UUID profileId = authContext.requireCurrentProfileId();
    var entity = new PhraseEntity();
    entity.setId(UUID.randomUUID());
    entity.setProfileId(profileId);
    entity.setText(text);
    entity.setCategory(category);
    entity.setIconUrl(iconUrl != null && !iconUrl.isBlank() ? iconUrl.trim() : null);
    return repo.save(entity);
  }

  @Transactional
  public PhraseEntity update(UUID id, String text, String category, String iconUrl) {
    UUID profileId = authContext.requireCurrentProfileId();
    var entity = repo.findById(id).orElseThrow(() -> new PhraseNotFoundException(id));
    if (!entity.getProfileId().equals(profileId)) {
      throw new PhraseNotFoundException(id);
    }
    entity.setText(text);
    entity.setCategory(category);
    entity.setIconUrl(iconUrl != null && !iconUrl.isBlank() ? iconUrl.trim() : null);
    return repo.save(entity);
  }

  @Transactional(readOnly = true)
  public PhraseEntity get(UUID id) {
    UUID profileId = authContext.requireCurrentProfileId();
    var entity = repo.findById(id).orElseThrow(() -> new PhraseNotFoundException(id));
    if (!entity.getProfileId().equals(profileId)) {
      throw new PhraseNotFoundException(id);
    }
    return entity;
  }


  @Transactional
  public void delete(UUID id) {
    UUID profileId = authContext.requireCurrentProfileId();
    var entity = repo.findById(id).orElseThrow(() -> new PhraseNotFoundException(id));
    if (!entity.getProfileId().equals(profileId)) {
      throw new PhraseNotFoundException(id);
    }
    repo.deleteById(id);
  }
}
