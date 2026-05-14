package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
    @NotBlank @Size(max = 150) String name,
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos.") String ruc,
    @Email @Size(max = 150) String email,
    @Size(max = 20) String phone,
    String address,
    @NotNull CatalogStatus status
) {}
