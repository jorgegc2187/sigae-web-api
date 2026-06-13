package com.sigae.api.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssetInventoryGroupResponse(
    String groupId,
    String displayName,
    UUID categoryId,
    String categoryIcon,
    String categoryName,
    UUID typeId,
    String typeName,
    int totalUnits,
    Instant lastEntryDate,
    List<AssetInventoryGroupUnitResponse> units
) {}
