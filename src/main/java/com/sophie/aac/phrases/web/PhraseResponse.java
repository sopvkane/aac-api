package com.sophie.aac.phrases.web;

import java.time.Instant;
import java.util.UUID;

public record PhraseResponse(
    UUID id,
    String text,
    Instant createdAt
) {}
