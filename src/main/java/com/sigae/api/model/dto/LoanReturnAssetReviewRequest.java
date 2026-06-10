package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetCondition;
import java.util.UUID;

public record LoanReturnAssetReviewRequest(
    UUID assetId,
    boolean hasIncident,
    String incidentDescription,
    AssetCondition conditionAfterReturn
) {}
