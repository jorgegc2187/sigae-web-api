package com.sigae.api.model.dto;

import java.time.Instant;
import java.util.UUID;

public record LoanActivityResponse(
    String id,
    String title,
    String description,
    String actor,
    Instant timestamp
) {
  public static LoanActivityResponse of(UUID id, String title, String description, String actor, Instant timestamp) {
    return new LoanActivityResponse(id.toString(), title, description, actor, timestamp);
  }
}
