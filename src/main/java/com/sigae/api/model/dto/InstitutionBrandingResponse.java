package com.sigae.api.model.dto;

import com.sigae.api.model.entity.InstitutionSettings;
import java.time.Instant;

public record InstitutionBrandingResponse(
    String systemName,
    boolean hasLogo,
    Instant updatedAt
) {
  public static InstitutionBrandingResponse from(InstitutionSettings settings) {
    return new InstitutionBrandingResponse(
        settings.getSystemName(),
        settings.hasLogo(),
        settings.getUpdatedAt()
    );
  }
}
