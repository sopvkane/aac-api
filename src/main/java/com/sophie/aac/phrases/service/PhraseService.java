package com.sophie.aac.phrases.service;

import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.repo.PhraseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PhraseService {

  private final PhraseRepository repo;

  public PhraseService(PhraseRepository repo) {
    this.repo = repo;
  }

  @Transactional(readOnly = true)
  public List<PhraseEntity> list() {
    return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  @Transactional
  public PhraseEntity create(String text) {
    var entity = new PhraseEntity(null, text.trim(), null);
    return repo.save(entity);
  }

  @Transactional
  public void delete(UUID id) {
    if (!repo.existsById(id)) {
      throw new PhraseNotFoundException(id);
    }
    repo.deleteById(id);
  }

  public static class PhraseNotFoundException extends RuntimeException {
    public PhraseNotFoundException(UUID id) {
      super("Phrase not found: " + id);
    }
  }
}
