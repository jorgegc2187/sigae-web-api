package com.sigae.api.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateAssetTypeRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 80) String icon,
    @Valid @NotEmpty List<AttributeDefinitionRequest> attributes
) {}
