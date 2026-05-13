package com.sigae.api.model.dto;

import com.sigae.api.model.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Email @Size(max = 320) String email,
    @NotNull UserRole role
) {}
