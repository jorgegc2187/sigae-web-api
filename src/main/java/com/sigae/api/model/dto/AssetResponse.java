package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssetResponse(
    UUID id,
    String code,
    String name,
    Instant createdAt,
    UUID assetTypeId,
    String assetTypeName,
    UUID categoryId,
    String categoryName,
    UUID locationId,
    String locationName,
    UUID supplierId,
    String supplierName,
    String createdByName,
    AssetCondition condition,
    String serialNumber,
    LocalDate acquisitionDate,
    Instant decommissionedAt,
    String notes,
    List<AssetAttributeValueResponse> attributeValues,
    List<AssetAttachmentResponse> attachments,
    boolean availableForLoan,
    UUID activeLoanId
) {
  public static AssetResponse from(Asset asset) {
    return from(asset, asset.getCondition() == AssetCondition.BUENO || asset.getCondition() == AssetCondition.REGULAR, null);
  }

  public static AssetResponse from(Asset asset, boolean availableForLoan, UUID activeLoanId) {
    return new AssetResponse(
        asset.getId(),
        asset.getCode(),
        asset.getName(),
        asset.getCreatedAt(),
        asset.getAssetType().getId(),
        asset.getAssetType().getName(),
        asset.getAssetType().getCategory().getId(),
        asset.getAssetType().getCategory().getName(),
        asset.getLocation().getId(),
        asset.getLocation().getName(),
        asset.getSupplier() == null ? null : asset.getSupplier().getId(),
        asset.getSupplier() == null ? null : asset.getSupplier().getName(),
        asset.getCreatedBy() == null ? null : asset.getCreatedBy().getFullName(),
        asset.getCondition(),
        asset.getSerialNumber(),
        asset.getAcquisitionDate(),
        asset.getDecommissionedAt(),
        asset.getNotes(),
        asset.getAttributeValues().stream().map(AssetAttributeValueResponse::from).toList(),
        asset.getAttachments().stream().map(AssetAttachmentResponse::from).toList(),
        availableForLoan,
        activeLoanId
    );
  }
}
