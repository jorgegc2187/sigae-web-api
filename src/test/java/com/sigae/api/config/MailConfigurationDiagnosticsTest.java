package com.sigae.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class MailConfigurationDiagnosticsTest {

  @Test
  void reportsMissingMailUsernameAndPasswordWhenAuthIsEnabled() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.mail.host", "smtp.gmail.com")
        .withProperty("spring.mail.port", "587")
        .withProperty("spring.mail.properties.mail.smtp.auth", "true");

    MailConfigurationDiagnostics diagnostics = new MailConfigurationDiagnostics(
        new AuthRecoveryProperties(true, "http://localhost:4200", "admin@sigae.edu.pe"),
        environment
    );

    List<String> missingProperties = diagnostics.detectMissingProperties();

    assertThat(missingProperties)
        .contains("MAIL_USERNAME", "MAIL_PASSWORD")
        .doesNotContain("MAIL_HOST", "MAIL_PORT", "MAIL_FROM");
  }

  @Test
  void ignoresMailChecksWhenAuthIsDisabledAndBaseValuesExist() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.mail.host", "localhost")
        .withProperty("spring.mail.port", "1025")
        .withProperty("spring.mail.properties.mail.smtp.auth", "false");

    MailConfigurationDiagnostics diagnostics = new MailConfigurationDiagnostics(
        new AuthRecoveryProperties(true, "http://localhost:4200", "no-reply@sigae.local"),
        environment
    );

    assertThat(diagnostics.detectMissingProperties()).isEmpty();
  }
}
