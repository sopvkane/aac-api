package com.sophie.aac.phrases.web;

import com.sophie.aac.phrases.domain.PhraseEntity;

import java.util.UUID;

public record PhraseResponse(
        UUID id,
        String text,
        String category
) {
    public static PhraseResponse from(PhraseEntity p) {
        return new PhraseResponse(p.getId(), p.getText(), p.getCategory());
    }
}
