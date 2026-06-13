package com.sigae.api.config;

import java.time.Duration;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
    Mfa mfa,
    @Valid
    Cors cors,
    @Valid
    AbuseProtection abuseProtection
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

  public record Mfa(
      @NotBlank
      String encryptionKey,
      @NotNull
      Duration challengeTtl,
      @Min(1)
      int maxAttempts,
      @NotBlank
      String issuer
  ) {}

  public record Cors(
      @NotEmpty
      List<String> allowedOrigins
  ) {}

  public record AbuseProtection(
      @Valid
      Login login,
      @Valid
      ForgotPassword forgotPassword,
      @Valid
      ResetPassword resetPassword,
      @Valid
      Refresh refresh,
      @Valid
      MfaProtection mfa
  ) {}

  public record Login(
      @Min(1)
      int accountMaxAttempts,
      @NotNull
      Duration accountWindow,
      @NotNull
      Duration lockDuration,
      @Min(1)
      int ipMaxAttempts,
      @NotNull
      Duration ipWindow
  ) {}

  public record ForgotPassword(
      @Min(1)
      int emailMaxAttempts,
      @NotNull
      Duration emailWindow,
      @Min(1)
      int ipMaxAttempts,
      @NotNull
      Duration ipWindow
  ) {}

  public record ResetPassword(
      @Min(1)
      int validateMaxAttempts,
      @NotNull
      Duration validateWindow,
      @Min(1)
      int submitMaxAttempts,
      @NotNull
      Duration submitWindow
  ) {}

  public record Refresh(
      @Min(1)
      int tokenMaxAttempts,
      @NotNull
      Duration tokenWindow
  ) {}

  public record MfaProtection(
      @Min(1)
      int startMaxAttempts,
      @NotNull
      Duration startWindow,
      @Min(1)
      int verifyMaxAttempts,
      @NotNull
      Duration verifyWindow
  ) {}
}
