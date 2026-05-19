package com.sigae.api.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateInstitutionSettingsRequest(
    @NotBlank @Size(max = 255) String systemName,
    @Size(max = 255) String address,
    @Size(max = 120) String city,
    @Size(max = 60) String supportPhone,
    @NotBlank @Email @Size(max = 255) String supportEmail
) {}
