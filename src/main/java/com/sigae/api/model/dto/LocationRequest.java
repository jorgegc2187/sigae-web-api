package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record LocationRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank String description,
    @NotNull CatalogStatus status,
    List<UUID> managerIds
) {}
