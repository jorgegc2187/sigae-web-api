package com.sigae.api.service;

import com.sigae.api.model.entity.InstitutionSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class InstitutionEmailTemplateService {

  private static final String DEFAULT_SYSTEM_NAME = "Sistema de Gestión de Activos";
  private static final String LOGO_CONTENT_ID = "institution-logo";

  private final InstitutionSettingsService institutionSettingsService;

  public InstitutionEmailTemplateService(InstitutionSettingsService institutionSettingsService) {
    this.institutionSettingsService = institutionSettingsService;
  }

  public InstitutionalEmail invitation(String recipientName, String actionUrl) {
    return build(
        recipientName,
        "Tu cuenta está lista",
        "Bienvenido/a",
        "Se creó una cuenta para ti. Configura tu contraseña para comenzar a usar el sistema.",
        "Configurar contraseña",
        "Si no esperabas este correo, comunícate con el equipo administrador.",
        actionUrl
    );
  }

  public InstitutionalEmail passwordReset(String recipientName, String actionUrl) {
    return build(
        recipientName,
        "Restablece tu contraseña",
        "Solicitud de recuperación",
        "Recibimos una solicitud para restablecer tu contraseña. Usa el siguiente enlace para continuar.",
        "Restablecer contraseña",
        "Si no solicitaste este cambio, puedes ignorar este mensaje con tranquilidad.",
        actionUrl
    );
  }

  private InstitutionalEmail build(
      String recipientName,
      String title,
      String eyebrow,
      String message,
      String actionLabel,
      String securityMessage,
      String actionUrl
  ) {
    InstitutionSettings settings = institutionSettingsService.getCurrentSettings();
    String systemName = normalize(settings.getSystemName(), DEFAULT_SYSTEM_NAME);
    String recipient = normalize(recipientName, "usuario/a");
    List<InlineEmailImage> inlineImages = buildInlineImages(settings);
    List<String> contactDetails = buildContactDetails(settings);
    String contactText = contactDetails.isEmpty()
        ? ""
        : "\n\nContacto: " + String.join(" · ", contactDetails);
    String logoHtml = inlineImages.isEmpty()
        ? "<div style=\"display:inline-block;padding:10px 14px;border-radius:10px;background:#ffffff;color:#1d4ed8;font-size:14px;font-weight:700;letter-spacing:.08em;\">SIGAE</div>"
        : "<img src=\"cid:" + LOGO_CONTENT_ID + "\" alt=\"Logo de " + escapeHtml(systemName)
            + "\" style=\"display:block;max-width:84px;max-height:64px;width:auto;height:auto;border:0;\" />";
    String contactHtml = contactDetails.isEmpty()
        ? ""
        : "<p style=\"margin:0;color:#64748b;font-size:12px;line-height:18px;\">"
            + escapeHtml(String.join(" · ", contactDetails)) + "</p>";
    String safeUrl = escapeHtml(actionUrl);
    String html = """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f1f5f9;padding:32px 12px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 8px 24px rgba(15,23,42,.10);">
                    <tr>
                      <td style="padding:24px 32px;background:#1d4ed8;">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                          <tr>
                            <td valign="middle">%s</td>
                            <td align="right" valign="middle" style="color:#dbeafe;font-size:13px;line-height:18px;">%s</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px 28px;">
                        <p style="margin:0 0 8px;color:#2563eb;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">%s</p>
                        <h1 style="margin:0 0 20px;font-size:26px;line-height:34px;color:#0f172a;">%s</h1>
                        <p style="margin:0 0 14px;font-size:16px;line-height:25px;color:#334155;">Hola, <strong>%s</strong>.</p>
                        <p style="margin:0 0 26px;font-size:16px;line-height:25px;color:#334155;">%s</p>
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                          <tr>
                            <td align="center" bgcolor="#1d4ed8" style="border-radius:10px;">
                              <a href="%s" style="display:inline-block;padding:13px 22px;color:#ffffff;font-size:15px;font-weight:700;text-decoration:none;">%s</a>
                            </td>
                          </tr>
                        </table>
                        <p style="margin:26px 0 0;padding-top:20px;border-top:1px solid #e2e8f0;font-size:13px;line-height:20px;color:#64748b;">%s</p>
                        <p style="margin:14px 0 0;font-size:12px;line-height:18px;color:#94a3b8;word-break:break-all;">Si el botón no funciona, copia y pega este enlace en tu navegador:<br><a href="%s" style="color:#2563eb;text-decoration:none;">%s</a></p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 32px;background:#f8fafc;border-top:1px solid #e2e8f0;text-align:center;">
                        <p style="margin:0 0 6px;color:#334155;font-size:13px;font-weight:700;">%s</p>
                        %s
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(
            logoHtml,
            escapeHtml(systemName),
            escapeHtml(eyebrow),
            escapeHtml(title),
            escapeHtml(recipient),
            escapeHtml(message),
            safeUrl,
            escapeHtml(actionLabel),
            escapeHtml(securityMessage),
            safeUrl,
            safeUrl,
            escapeHtml(systemName),
            contactHtml
        );
    String text = """
        %s

        %s

        Hola, %s.

        %s

        %s:
        %s

        %s%s
        """.formatted(systemName, title, recipient, message, actionLabel, actionUrl, securityMessage, contactText);
    return new InstitutionalEmail(systemName, text, html, inlineImages);
  }

  private List<InlineEmailImage> buildInlineImages(InstitutionSettings settings) {
    String contentType = normalize(settings.getLogoMimeType(), null);
    if (!settings.hasLogo() || contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      return List.of();
    }

    return List.of(new InlineEmailImage(
        LOGO_CONTENT_ID,
        normalizeFileName(settings.getLogoFileName()),
        contentType,
        settings.getLogoContent()
    ));
  }

  private List<String> buildContactDetails(InstitutionSettings settings) {
    List<String> details = new ArrayList<>();
    addIfPresent(details, settings.getAddress());
    addIfPresent(details, settings.getCity());
    String phone = normalize(settings.getSupportPhone(), null);
    if (phone != null) {
      details.add("Tel. " + phone);
    }
    addIfPresent(details, settings.getSupportEmail());
    return details;
  }

  private void addIfPresent(List<String> values, String value) {
    String normalized = normalize(value, null);
    if (normalized != null) {
      values.add(normalized);
    }
  }

  private String normalize(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String normalizeFileName(String value) {
    String normalized = normalize(value, "institution-logo");
    return normalized.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record InstitutionalEmail(
      String systemName,
      String text,
      String html,
      List<InlineEmailImage> inlineImages
  ) {}
}
