package com.sigae.api.model.dto;

import java.time.Instant;

public record UserMfaStatusResponse(
    boolean mfaRequired,
    boolean mfaEnabled,
    Instant mfaEnabledAt
) {

  public static UserMfaStatusResponse disabled() {
    return new UserMfaStatusResponse(false, false, null);
  }
}
