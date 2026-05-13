package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 80) String icon
) {}
