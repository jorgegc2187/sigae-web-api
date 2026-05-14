package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssetRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 160) String name,
    @NotNull UUID assetTypeId,
    @NotNull UUID locationId,
    UUID supplierId,
    @NotNull AssetCondition condition,
    @Size(max = 100) String serialNumber,
    @Size(max = 100) String barcode,
    LocalDate acquisitionDate,
    String notes,
    @Valid List<AssetAttributeValueRequest> attributeValues
) {}
