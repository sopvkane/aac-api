package com.sophie.aac.phrases.web;

import com.sophie.aac.phrases.domain.PhraseEntity;

import java.util.UUID;

public record PhraseResponse(UUID id, String text, String category, String iconUrl, boolean requiresPersonSelection) {

  public static PhraseResponse from(PhraseEntity entity) {
    String cat = entity.getCategory() == null ? "" : entity.getCategory().toLowerCase();
    boolean needsPerson = cat.contains("request") || cat.contains("ask") || cat.contains("favor")
        || cat.contains("want") || cat.contains("watch") || cat.contains("play");
    return new PhraseResponse(entity.getId(), entity.getText(), entity.getCategory(), entity.getIconUrl(), needsPerson);
  }
}
