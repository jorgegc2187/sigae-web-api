package com.sigae.api.model.dto;

import com.sigae.api.model.entity.InstitutionSettings;
import java.time.Instant;

public record InstitutionSettingsResponse(
    String systemName,
    String address,
    String city,
    String supportPhone,
    String supportEmail,
    boolean hasLogo,
    Instant updatedAt
) {
  public static InstitutionSettingsResponse from(InstitutionSettings settings) {
    return new InstitutionSettingsResponse(
        settings.getSystemName(),
        settings.getAddress(),
        settings.getCity(),
        settings.getSupportPhone(),
        settings.getSupportEmail(),
        settings.hasLogo(),
        settings.getUpdatedAt()
    );
  }
}
