package com.sophie.aac.phrases.domain;

import java.util.UUID;

public class PhraseNotFoundException extends RuntimeException {
    public PhraseNotFoundException(UUID id) {
        super("Phrase not found: " + id);
    }
}
