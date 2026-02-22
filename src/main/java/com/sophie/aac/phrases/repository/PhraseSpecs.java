package com.sophie.aac.phrases.repository;

import com.sophie.aac.phrases.domain.PhraseEntity;
import org.springframework.data.jpa.domain.Specification;

public final class PhraseSpecs {

    private PhraseSpecs() {}

    public static Specification<PhraseEntity> textContainsIgnoreCase(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("text")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<PhraseEntity> categoryEqualsIgnoreCase(String category) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }
}
