package com.sophie.aac.phrases.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePhraseRequest(
    @NotBlank @Size(max = 280) String text
) {}
