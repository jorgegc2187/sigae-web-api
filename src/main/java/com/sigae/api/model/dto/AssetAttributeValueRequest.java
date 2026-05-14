package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssetAttributeValueRequest(
    @NotNull UUID attributeDefinitionId,
    @NotBlank String value
) {}
