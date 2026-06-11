package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssetStatusChangeRequest(
    @NotNull AssetCondition nextCondition,
    @NotBlank String reason
) {}
