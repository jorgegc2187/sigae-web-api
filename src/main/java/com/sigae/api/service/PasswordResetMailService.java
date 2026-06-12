package com.sigae.api.service;

import com.sigae.api.config.AuthRecoveryProperties;
import com.sigae.api.model.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

  private static final String DEFAULT_VISIBLE_SYSTEM_NAME = "Sistema de Gestión de Activos";

  private final JavaMailSender mailSender;
  private final AuthRecoveryProperties recoveryProperties;
  private final InstitutionSettingsService institutionSettingsService;

  public PasswordResetMailService(
      JavaMailSender mailSender,
      AuthRecoveryProperties recoveryProperties,
      InstitutionSettingsService institutionSettingsService
  ) {
    this.mailSender = mailSender;
    this.recoveryProperties = recoveryProperties;
    this.institutionSettingsService = institutionSettingsService;
  }

  public void sendPasswordResetMail(User user, String rawToken) {
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
      helper.setSubject("Restablecimiento de contraseña - " + systemName);
      helper.setText(buildPlainText(resetUrl, systemName), buildHtml(resetUrl, systemName));
      mailSender.send(message);
    } catch (MessagingException | RuntimeException exception) {
      throw new IllegalStateException("No se pudo enviar el correo de recuperación.", exception);
    }
  }

  private String buildResetUrl(String rawToken) {
    String frontendUrl = recoveryProperties.frontendUrl().replaceAll("/+$", "");
    return frontendUrl + "/auth/reset-password?token="
        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
  }

  private String buildPlainText(String resetUrl, String systemName) {
    return """
        Recibimos una solicitud para restablecer tu contraseña de %s.

        Usa el siguiente enlace para continuar:
        %s

        Si no solicitaste este cambio, puedes ignorar este mensaje.
        """.formatted(systemName, resetUrl);
  }

  private String buildHtml(String resetUrl, String systemName) {
    return """
        <html lang="es">
          <body style="font-family: Arial, sans-serif; color: #0f172a;">
            <p>Recibimos una solicitud para restablecer tu contraseña de <strong>%s</strong>.</p>
            <p>
              <a href="%s" style="display: inline-block; padding: 12px 20px; border-radius: 10px; background: #1d4ed8; color: #ffffff; text-decoration: none; font-weight: 600;">
                Restablecer contraseña
              </a>
            </p>
            <p>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>
          </body>
        </html>
        """.formatted(systemName, resetUrl);
  }

  private String resolveVisibleSystemName() {
    String configuredSystemName = institutionSettingsService.getCurrentSettings().getSystemName();
    if (configuredSystemName == null || configuredSystemName.isBlank()) {
      return DEFAULT_VISIBLE_SYSTEM_NAME;
    }

    return configuredSystemName.trim();
  }
}
