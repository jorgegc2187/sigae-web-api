package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
    @NotBlank(message = "El token de verificación es obligatorio.")
    String challengeToken,

    @NotBlank(message = "El código 2FA es obligatorio.")
    @Pattern(regexp = "\\d{6}", message = "El código 2FA debe tener 6 dígitos.")
    String code
) {}
