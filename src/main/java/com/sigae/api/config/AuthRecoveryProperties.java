package com.sigae.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth.recovery")
public record AuthRecoveryProperties(
    boolean mailEnabled,
    @NotBlank String frontendUrl,
    @NotBlank String mailFrom
) {}
