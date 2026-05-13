package com.sigae.api.config;

import java.time.Duration;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    @Valid
    Jwt jwt,
    @Valid
    Cors cors
) {

  public record Jwt(
      @NotBlank
      String issuer,
      @NotBlank
      @Size(min = 32)
      String secret,
      @NotNull
      Duration accessTokenTtl,
      @NotNull
      Duration refreshTokenTtl,
      @NotNull
      Duration passwordResetTokenTtl
  ) {}

  public record Cors(
      @NotEmpty
      List<String> allowedOrigins
  ) {}
}
