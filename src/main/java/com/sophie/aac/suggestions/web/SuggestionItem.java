package com.sophie.aac.suggestions.web;

import com.sophie.aac.phrases.web.PhraseResponse;

public record SuggestionItem(PhraseResponse phrase, double score) {}