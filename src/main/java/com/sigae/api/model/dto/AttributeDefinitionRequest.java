package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AttributeDefinitionRequest(
    UUID id,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 255) String description,
    boolean isRequired
) {}
