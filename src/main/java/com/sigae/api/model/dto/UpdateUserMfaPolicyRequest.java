package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserMfaPolicyRequest(
    @NotNull(message = "Debe indicar si 2FA es requerido.")
    Boolean mfaRequired
) {}
