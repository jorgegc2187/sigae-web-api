package com.sigae.api.service;

import com.sigae.api.config.AuthRecoveryProperties;
import com.sigae.api.exception.MailDeliveryException;
import com.sigae.api.model.entity.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

  private final EmailDeliveryService emailDeliveryService;
  private final AuthRecoveryProperties recoveryProperties;
  private final InstitutionEmailTemplateService institutionEmailTemplateService;

  public PasswordResetMailService(
      EmailDeliveryService emailDeliveryService,
      AuthRecoveryProperties recoveryProperties,
      InstitutionEmailTemplateService institutionEmailTemplateService
  ) {
    this.emailDeliveryService = emailDeliveryService;
    this.recoveryProperties = recoveryProperties;
    this.institutionEmailTemplateService = institutionEmailTemplateService;
  }

  public void sendPasswordResetMail(User user, String rawToken) {
    if (!recoveryProperties.mailEnabled()) {
      return;
    }

    try {
      String resetUrl = buildResetUrl(rawToken);
      InstitutionEmailTemplateService.InstitutionalEmail template = institutionEmailTemplateService.passwordReset(
          user.getFullName(),
          resetUrl
      );
      emailDeliveryService.send(new EmailMessage(
          recoveryProperties.mailFrom(),
          user.getEmail(),
          "Restablecimiento de contraseña - " + template.systemName(),
          template.text(),
          template.html(),
          template.inlineImages()
      ));
    } catch (RuntimeException exception) {
      throw new MailDeliveryException("No se pudo enviar el correo de recuperación.", exception);
    }
  }

  private String buildResetUrl(String rawToken) {
    String frontendUrl = recoveryProperties.frontendUrl().replaceAll("/+$", "");
    return frontendUrl + "/auth/reset-password?token="
        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
  }

}
