package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
    @NotBlank @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String dni,
    @NotBlank @Size(max = 160) String fullName,
    @Size(max = 120) String specialty,
    @Email @Size(max = 150) String email,
    @Size(max = 20) String phone,
    @NotNull CatalogStatus status
) {}
