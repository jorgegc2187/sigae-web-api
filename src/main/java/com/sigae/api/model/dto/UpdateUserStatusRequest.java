package com.sigae.api.model.dto;

import com.sigae.api.model.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull UserStatus status
) {}
