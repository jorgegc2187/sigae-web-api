package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssetResponse(
    UUID id,
    String code,
    String name,
    UUID assetTypeId,
    String assetTypeName,
    UUID categoryId,
    String categoryName,
    UUID locationId,
    String locationName,
    UUID supplierId,
    String supplierName,
    AssetCondition condition,
    String serialNumber,
    String barcode,
    LocalDate acquisitionDate,
    String notes,
    List<AssetAttributeValueResponse> attributeValues
) {
  public static AssetResponse from(Asset asset) {
    return new AssetResponse(
        asset.getId(),
        asset.getCode(),
        asset.getName(),
        asset.getAssetType().getId(),
        asset.getAssetType().getName(),
        asset.getAssetType().getCategory().getId(),
        asset.getAssetType().getCategory().getName(),
        asset.getLocation().getId(),
        asset.getLocation().getName(),
        asset.getSupplier() == null ? null : asset.getSupplier().getId(),
        asset.getSupplier() == null ? null : asset.getSupplier().getName(),
        asset.getCondition(),
        asset.getSerialNumber(),
        asset.getBarcode(),
        asset.getAcquisitionDate(),
        asset.getNotes(),
        asset.getAttributeValues().stream().map(AssetAttributeValueResponse::from).toList()
    );
  }
}
