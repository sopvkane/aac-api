package com.sophie.aac.phrases.repo;

import com.sophie.aac.phrases.domain.PhraseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PhraseRepository extends JpaRepository<PhraseEntity, UUID> {}
