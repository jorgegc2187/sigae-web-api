package com.sigae.api.service;

import com.sigae.api.config.AuthRecoveryProperties;
import com.sigae.api.exception.MailDeliveryException;
import com.sigae.api.model.entity.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserInvitationMailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserInvitationMailService.class);
  private final EmailDeliveryService emailDeliveryService;
  private final AuthRecoveryProperties recoveryProperties;
  private final InstitutionEmailTemplateService institutionEmailTemplateService;

  public UserInvitationMailService(
      EmailDeliveryService emailDeliveryService,
      AuthRecoveryProperties recoveryProperties,
      InstitutionEmailTemplateService institutionEmailTemplateService
  ) {
    this.emailDeliveryService = emailDeliveryService;
    this.recoveryProperties = recoveryProperties;
    this.institutionEmailTemplateService = institutionEmailTemplateService;
  }

  public void sendInvitationMail(User user, String rawToken) {
    if (!recoveryProperties.mailEnabled()) {
      return;
    }

    try {
      String resetUrl = buildResetUrl(rawToken);
      InstitutionEmailTemplateService.InstitutionalEmail template = institutionEmailTemplateService.invitation(
          user.getFullName(),
          resetUrl
      );
      emailDeliveryService.send(new EmailMessage(
          recoveryProperties.mailFrom(),
          user.getEmail(),
          "Bienvenido a " + template.systemName() + " - Configura tu contraseña",
          template.text(),
          template.html(),
          template.inlineImages()
      ));
    } catch (RuntimeException exception) {
      LOGGER.error(
          "No se pudo enviar el correo de invitación a {} usando el proveedor configurado.",
          user.getEmail(),
          exception
      );
      throw new MailDeliveryException(
          "No se pudo enviar el correo de invitación. Verifique la configuración de correo e intente nuevamente.",
          exception
      );
    }
  }

  private String buildResetUrl(String rawToken) {
    String frontendUrl = recoveryProperties.frontendUrl().replaceAll("/+$", "");
    return frontendUrl + "/auth/reset-password?token="
        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
  }

}
