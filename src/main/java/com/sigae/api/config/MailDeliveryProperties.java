package com.sigae.api.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailDeliveryProperties(
    @NotNull Provider provider,
    Resend resend
) {

  public enum Provider {
    SMTP,
    RESEND
  }

  public record Resend(
      String apiKey
  ) {}
}
