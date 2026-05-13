package com.sigae.api.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateAssetTypeRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 80) String icon,
    @Valid @NotEmpty List<AttributeDefinitionRequest> attributes
) {}
