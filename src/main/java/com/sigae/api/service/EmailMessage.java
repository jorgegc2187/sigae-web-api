package com.sigae.api.service;

import java.util.List;

public record EmailMessage(
    String from,
    String to,
    String subject,
    String text,
    String html,
    List<InlineEmailImage> inlineImages
) {

  public EmailMessage {
    inlineImages = inlineImages == null ? List.of() : List.copyOf(inlineImages);
  }

  public EmailMessage(String from, String to, String subject, String text, String html) {
    this(from, to, subject, text, html, List.of());
  }
}
