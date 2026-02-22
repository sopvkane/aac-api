package com.sophie.aac.phrases.repository;

import com.sophie.aac.phrases.domain.PhraseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface PhraseRepository
    extends JpaRepository<PhraseEntity, UUID>, JpaSpecificationExecutor<PhraseEntity> {
        List<PhraseEntity> findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc(String prefix);
        List<PhraseEntity> findTop50ByTextContainingIgnoreCaseOrderByTextAsc(String infix);
}
