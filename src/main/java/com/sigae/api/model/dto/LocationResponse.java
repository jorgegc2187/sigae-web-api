package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Location;
import java.util.List;
import java.util.UUID;

public record LocationResponse(
    UUID id,
    String name,
    String description,
    CatalogStatus status,
    List<LocationManagerResponse> managers
) {
  public static LocationResponse from(Location location, List<LocationManagerResponse> managers) {
    return new LocationResponse(
        location.getId(),
        location.getName(),
        location.getDescription(),
        location.getStatus(),
        managers
    );
  }
}
