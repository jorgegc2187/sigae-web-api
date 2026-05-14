package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import java.time.LocalDate;
import java.util.UUID;

public record AssetReportRowResponse(
    UUID id,
    String code,
    String description,
    String category,
    UUID categoryId,
    String location,
    UUID locationId,
    AssetCondition condition,
    LocalDate acquisitionDate
) {
  public static AssetReportRowResponse from(Asset asset) {
    return new AssetReportRowResponse(
        asset.getId(),
        asset.getCode(),
        asset.getName(),
        asset.getAssetType().getCategory().getName(),
        asset.getAssetType().getCategory().getId(),
        asset.getLocation().getName(),
        asset.getLocation().getId(),
        asset.getCondition(),
        asset.getAcquisitionDate()
    );
  }
}
