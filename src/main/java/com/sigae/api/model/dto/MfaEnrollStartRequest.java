package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaEnrollStartRequest(
    @NotBlank(message = "El token de verificación es obligatorio.")
    String challengeToken
) {}
