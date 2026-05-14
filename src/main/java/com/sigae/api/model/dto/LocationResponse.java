package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Location;
import java.util.UUID;

public record LocationResponse(
    UUID id,
    String name,
    String description,
    CatalogStatus status
) {
  public static LocationResponse from(Location location) {
    return new LocationResponse(
        location.getId(),
        location.getName(),
        location.getDescription(),
        location.getStatus()
    );
  }
}
