package com.sophie.aac.phrases.service;

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

  public PhraseService(PhraseRepository repo) {
    this.repo = repo;
  }

  @Transactional(readOnly = true)
  public List<PhraseEntity> list(Optional<String> q, Optional<String> category) {
    Specification<PhraseEntity> spec = (root, query, cb) -> cb.conjunction();

    if (q.isPresent() && !q.get().isBlank()) {
      spec = spec.and(PhraseSpecs.textContainsIgnoreCase(q.get()));
    }
    if (category.isPresent() && !category.get().isBlank()) {
      spec = spec.and(PhraseSpecs.categoryEqualsIgnoreCase(category.get()));
    }

    return repo.findAll(spec, Sort.by(Sort.Direction.ASC, "text"));
  }

  @Transactional
  public PhraseEntity create(String text, String category) {
    var entity = new PhraseEntity();
    entity.setId(UUID.randomUUID());
    entity.setText(text);
    entity.setCategory(category);
    return repo.save(entity);
  }

  @Transactional
  public PhraseEntity update(UUID id, String text, String category) {
    var entity = repo.findById(id).orElseThrow(() -> new PhraseNotFoundException(id));
    entity.setText(text);
    entity.setCategory(category);
    return repo.save(entity);
  }

  @Transactional(readOnly = true)
  public PhraseEntity get(UUID id) {
    return repo.findById(id).orElseThrow(() -> new PhraseNotFoundException(id));
  }


  @Transactional
  public void delete(UUID id) {
    if (!repo.existsById(id)) {
      throw new PhraseNotFoundException(id);
    }
    repo.deleteById(id);
  }
}
