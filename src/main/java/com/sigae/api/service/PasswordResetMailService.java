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

  private final JavaMailSender mailSender;
  private final AuthRecoveryProperties recoveryProperties;

  public PasswordResetMailService(
      JavaMailSender mailSender,
      AuthRecoveryProperties recoveryProperties
  ) {
    this.mailSender = mailSender;
    this.recoveryProperties = recoveryProperties;
  }

  public void sendPasswordResetMail(User user, String rawToken) {
    if (!recoveryProperties.mailEnabled()) {
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      String resetUrl = buildResetUrl(rawToken);
      helper.setFrom(recoveryProperties.mailFrom());
      helper.setTo(user.getEmail());
      helper.setSubject("Restablecimiento de contraseña - SIGAE");
      helper.setText(buildPlainText(resetUrl), buildHtml(resetUrl));
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

  private String buildPlainText(String resetUrl) {
    return """
        Recibimos una solicitud para restablecer tu contraseña de SIGAE.

        Usa el siguiente enlace para continuar:
        %s

        Si no solicitaste este cambio, puedes ignorar este mensaje.
        """.formatted(resetUrl);
  }

  private String buildHtml(String resetUrl) {
    return """
        <html lang="es">
          <body style="font-family: Arial, sans-serif; color: #0f172a;">
            <p>Recibimos una solicitud para restablecer tu contraseña de <strong>SIGAE</strong>.</p>
            <p>
              <a href="%s" style="display: inline-block; padding: 12px 20px; border-radius: 10px; background: #1d4ed8; color: #ffffff; text-decoration: none; font-weight: 600;">
                Restablecer contraseña
              </a>
            </p>
            <p>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>
          </body>
        </html>
        """.formatted(resetUrl);
  }
}
