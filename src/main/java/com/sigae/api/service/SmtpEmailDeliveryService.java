package com.sigae.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailDeliveryService implements EmailDeliveryService {

  private final JavaMailSender mailSender;

  public SmtpEmailDeliveryService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void send(EmailMessage message) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
      helper.setFrom(message.from());
      helper.setTo(message.to());
      helper.setSubject(message.subject());
      helper.setText(message.text(), message.html());
      for (InlineEmailImage image : message.inlineImages()) {
        helper.addInline(
            image.contentId(),
            new ByteArrayResource(image.content()),
            image.contentType()
        );
      }
      mailSender.send(mimeMessage);
    } catch (MessagingException | RuntimeException exception) {
      throw new IllegalStateException("No se pudo enviar el correo mediante SMTP.", exception);
    }
  }
}
