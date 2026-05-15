package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "El token es requerido.")
    String token,
    @NotBlank(message = "La nueva contraseña es requerida.")
    String newPassword,
    @NotBlank(message = "La confirmación de contraseña es requerida.")
    String confirmPassword
) {}
