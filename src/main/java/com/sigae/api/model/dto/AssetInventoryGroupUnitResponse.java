package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import java.time.Instant;
import java.util.UUID;

public record AssetInventoryGroupUnitResponse(
    UUID id,
    String code,
    String locationName,
    AssetCondition condition,
    Instant lastInspectionDate
) {
  public static AssetInventoryGroupUnitResponse from(Asset asset) {
    return new AssetInventoryGroupUnitResponse(
        asset.getId(),
        asset.getCode(),
        asset.getLocation().getName(),
        asset.getCondition(),
        asset.getUpdatedAt()
    );
  }
}
