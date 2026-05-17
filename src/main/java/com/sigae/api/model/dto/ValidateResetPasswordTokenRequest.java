package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateResetPasswordTokenRequest(
    @NotBlank(message = "El token es requerido.")
    String token
) {}
