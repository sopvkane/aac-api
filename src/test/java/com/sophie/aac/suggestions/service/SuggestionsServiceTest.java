package com.sophie.aac.suggestions.service;

import com.sophie.aac.phrases.domain.PhraseEntity;
import com.sophie.aac.phrases.repository.PhraseRepository;
import com.sophie.aac.suggestions.domain.LocationCategory;
import com.sophie.aac.suggestions.domain.TimeBucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SuggestionsServiceTest {

  private PhraseRepository phraseRepository;
  private SuggestionsService suggestionsService;

  @BeforeEach
  void setUp() {
    phraseRepository = mock(PhraseRepository.class);
    suggestionsService = new SuggestionsService(phraseRepository);
  }

  @Test
  void suggest_with_prefix_prefers_startsWith_over_contains_and_returns_limit() {
    PhraseEntity startsWith = phrase("Can you help me?", "general");
    PhraseEntity contains = phrase("Could you can help?", "general"); // contains "can" but not startsWith

    when(phraseRepository.findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc("can"))
        .thenReturn(List.of(startsWith));
    when(phraseRepository.findTop50ByTextContainingIgnoreCaseOrderByTextAsc("can"))
        .thenReturn(List.of(contains));

    var result = suggestionsService.suggest("can", TimeBucket.MORNING, LocationCategory.HOME, 1);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).phrase().text(), is("Can you help me?"));
    assertThat(result.get(0).score(), greaterThan(0.6)); // startsWith should be 1.0

    verify(phraseRepository).findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc("can");
    verify(phraseRepository).findTop50ByTextContainingIgnoreCaseOrderByTextAsc("can");
    verify(phraseRepository, never()).findAll(ArgumentMatchers.any(Sort.class));
  }

  @Test
  void suggest_with_empty_prefix_uses_findAll_sorted_and_returns_limit() {
    PhraseEntity a = phrase("A", "x");
    PhraseEntity b = phrase("B", "x");
    PhraseEntity c = phrase("C", "x");

    when(phraseRepository.findAll(Sort.by("text").ascending()))
        .thenReturn(List.of(a, b, c));

    var result = suggestionsService.suggest("   ", TimeBucket.NIGHT, LocationCategory.OTHER, 2);

    assertThat(result, hasSize(2));
    assertThat(result.get(0).phrase().text(), is("A"));
    assertThat(result.get(1).phrase().text(), is("B"));

    verify(phraseRepository).findAll(Sort.by("text").ascending());
    verify(phraseRepository, never()).findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc(anyString());
    verify(phraseRepository, never()).findTop50ByTextContainingIgnoreCaseOrderByTextAsc(anyString());
  }

  @Test
  void suggest_de_dupes_candidates_by_id_preserving_first_occurrence() {
    UUID id = UUID.randomUUID();

    PhraseEntity p1 = new PhraseEntity();
    p1.setId(id);
    p1.setText("Can you help me?");
    p1.setCategory("general");

    PhraseEntity p1Duplicate = new PhraseEntity();
    p1Duplicate.setId(id); // same id
    p1Duplicate.setText("Can you help me? (duplicate)");
    p1Duplicate.setCategory("general");

    when(phraseRepository.findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc("can"))
        .thenReturn(List.of(p1));
    when(phraseRepository.findTop50ByTextContainingIgnoreCaseOrderByTextAsc("can"))
        .thenReturn(List.of(p1Duplicate));

    var result = suggestionsService.suggest("can", TimeBucket.MORNING, LocationCategory.HOME, 10);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).phrase().id(), is(id));
    assertThat(result.get(0).phrase().text(), is("Can you help me?"));

    verify(phraseRepository).findTop50ByTextStartingWithIgnoreCaseOrderByTextAsc("can");
    verify(phraseRepository).findTop50ByTextContainingIgnoreCaseOrderByTextAsc("can");
  }

  private static PhraseEntity phrase(String text, String category) {
    PhraseEntity p = new PhraseEntity();
    p.setId(UUID.randomUUID());
    p.setText(text);
    p.setCategory(category);
    return p;
  }
}