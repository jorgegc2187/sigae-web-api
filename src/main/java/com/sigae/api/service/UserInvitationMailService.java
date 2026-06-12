package com.sigae.api.service;

import com.sigae.api.config.AuthRecoveryProperties;
import com.sigae.api.exception.MailDeliveryException;
import com.sigae.api.model.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class UserInvitationMailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserInvitationMailService.class);
  private static final String DEFAULT_VISIBLE_SYSTEM_NAME = "Sistema de Gestión de Activos";

  private final JavaMailSender mailSender;
  private final AuthRecoveryProperties recoveryProperties;
  private final InstitutionSettingsService institutionSettingsService;

  public UserInvitationMailService(
      JavaMailSender mailSender,
      AuthRecoveryProperties recoveryProperties,
      InstitutionSettingsService institutionSettingsService
  ) {
    this.mailSender = mailSender;
    this.recoveryProperties = recoveryProperties;
    this.institutionSettingsService = institutionSettingsService;
  }

  public void sendInvitationMail(User user, String rawToken) {
    if (!recoveryProperties.mailEnabled()) {
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      String resetUrl = buildResetUrl(rawToken);
      String systemName = resolveVisibleSystemName();
      helper.setFrom(recoveryProperties.mailFrom());
      helper.setTo(user.getEmail());
      helper.setSubject("Bienvenido a " + systemName + " - Configura tu contraseña");
      helper.setText(
          buildPlainText(user.getFullName(), resetUrl, systemName),
          buildHtml(user.getFullName(), resetUrl, systemName)
      );
      mailSender.send(message);
    } catch (MessagingException | RuntimeException exception) {
      LOGGER.error(
          "No se pudo enviar el correo de invitación a {} usando el host SMTP configurado.",
          user.getEmail(),
          exception
      );
      throw new MailDeliveryException(
          "No se pudo enviar el correo de invitación. Verifique la configuración SMTP e intente nuevamente.",
          exception
      );
    }
  }

  private String buildResetUrl(String rawToken) {
    String frontendUrl = recoveryProperties.frontendUrl().replaceAll("/+$", "");
    return frontendUrl + "/auth/reset-password?token="
        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
  }

  private String buildPlainText(String fullName, String resetUrl, String systemName) {
    return """
        Hola %s,

        Se creó una cuenta para ti en %s. Usa el siguiente enlace para configurar tu contraseña de acceso:
        %s

        Si no esperabas este correo, contacta al administrador del sistema.
        """.formatted(fullName, systemName, resetUrl);
  }

  private String buildHtml(String fullName, String resetUrl, String systemName) {
    return """
        <html lang="es">
          <body style="font-family: Arial, sans-serif; color: #0f172a;">
            <p>Hola <strong>%s</strong>,</p>
            <p>Se creó una cuenta para ti en <strong>%s</strong>. Usa el siguiente enlace para configurar tu contraseña de acceso.</p>
            <p>
              <a href="%s" style="display: inline-block; padding: 12px 20px; border-radius: 10px; background: #1d4ed8; color: #ffffff; text-decoration: none; font-weight: 600;">
                Configurar contraseña
              </a>
            </p>
            <p>Si no esperabas este correo, contacta al administrador del sistema.</p>
          </body>
        </html>
        """.formatted(fullName, systemName, resetUrl);
  }

  private String resolveVisibleSystemName() {
    String configuredSystemName = institutionSettingsService.getCurrentSettings().getSystemName();
    if (configuredSystemName == null || configuredSystemName.isBlank()) {
      return DEFAULT_VISIBLE_SYSTEM_NAME;
    }

    return configuredSystemName.trim();
  }
}
