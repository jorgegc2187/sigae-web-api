package com.sigae.api.service;

import com.sigae.api.config.MailDeliveryProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "resend")
public class ResendEmailDeliveryService implements EmailDeliveryService {

  private final RestClient restClient;

  public ResendEmailDeliveryService(MailDeliveryProperties mailDeliveryProperties) {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    );
    requestFactory.setReadTimeout(Duration.ofSeconds(15));
    this.restClient = RestClient.builder()
        .baseUrl("https://api.resend.com")
        .requestFactory(requestFactory)
        .defaultHeader("Authorization", "Bearer " + mailDeliveryProperties.resend().apiKey())
        .build();
  }

  @Override
  public void send(EmailMessage message) {
    try {
      restClient.post()
          .uri("/emails")
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of(
              "from", message.from(),
              "to", new String[] {message.to()},
              "subject", message.subject(),
              "text", message.text(),
              "html", message.html()
          ))
          .retrieve()
          .toBodilessEntity();
    } catch (RuntimeException exception) {
      throw new IllegalStateException("No se pudo enviar el correo mediante Resend.", exception);
    }
  }
}
