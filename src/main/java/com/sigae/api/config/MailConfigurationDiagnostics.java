package com.sigae.api.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MailConfigurationDiagnostics {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailConfigurationDiagnostics.class);

  private final MailDeliveryProperties deliveryProperties;
  private final AuthRecoveryProperties recoveryProperties;
  private final Environment environment;

  public MailConfigurationDiagnostics(
      MailDeliveryProperties deliveryProperties,
      AuthRecoveryProperties recoveryProperties,
      Environment environment
  ) {
    this.deliveryProperties = deliveryProperties;
    this.recoveryProperties = recoveryProperties;
    this.environment = environment;
  }

  public void logIfMisconfigured() {
    if (!recoveryProperties.mailEnabled()) {
      return;
    }

    List<String> missingProperties = detectMissingProperties();
    if (!missingProperties.isEmpty()) {
      LOGGER.warn(
          "MAIL_ENABLED=true pero faltan propiedades SMTP requeridas para el envio de correos: {}",
          String.join(", ", missingProperties)
      );
    }
  }

  List<String> detectMissingProperties() {
    List<String> missingProperties = new ArrayList<>();

    require("MAIL_FROM", recoveryProperties.mailFrom(), missingProperties);
    if (deliveryProperties.provider() == MailDeliveryProperties.Provider.RESEND) {
      require("RESEND_API_KEY", environment.getProperty("RESEND_API_KEY"), missingProperties);
      return missingProperties;
    }

    require("MAIL_HOST", environment.getProperty("spring.mail.host"), missingProperties);
    require("MAIL_PORT", environment.getProperty("spring.mail.port"), missingProperties);

    boolean authEnabled = environment.getProperty("spring.mail.properties.mail.smtp.auth", Boolean.class, false);
    if (authEnabled) {
      require("MAIL_USERNAME", environment.getProperty("spring.mail.username"), missingProperties);
      require("MAIL_PASSWORD", environment.getProperty("spring.mail.password"), missingProperties);
    }

    return missingProperties;
  }

  private void require(String propertyName, String value, List<String> missingProperties) {
    if (value == null || value.isBlank()) {
      missingProperties.add(propertyName);
    }
  }
}
