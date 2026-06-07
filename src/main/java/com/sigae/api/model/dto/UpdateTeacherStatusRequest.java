package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTeacherStatusRequest(
    @NotNull CatalogStatus status
) {}
