package com.sigae.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupDiagnosticsRunner implements ApplicationRunner {

  private final MailConfigurationDiagnostics mailConfigurationDiagnostics;

  public StartupDiagnosticsRunner(MailConfigurationDiagnostics mailConfigurationDiagnostics) {
    this.mailConfigurationDiagnostics = mailConfigurationDiagnostics;
  }

  @Override
  public void run(ApplicationArguments args) {
    mailConfigurationDiagnostics.logIfMisconfigured();
  }
}
