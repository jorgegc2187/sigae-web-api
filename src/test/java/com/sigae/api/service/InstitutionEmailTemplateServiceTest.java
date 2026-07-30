package com.sigae.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sigae.api.model.entity.InstitutionSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionEmailTemplateServiceTest {

  @Mock
  InstitutionSettingsService institutionSettingsService;

  @InjectMocks
  InstitutionEmailTemplateService templateService;

  @Test
  void includesConfiguredLogoAndContactDetailsWhenPresent() {
    InstitutionSettings settings = new InstitutionSettings(
        "I.E. Simón Rodríguez",
        "Av. Los Educadores 123",
        "Nasca",
        "987 654 321",
        "soporte@simonrodriguez.lat"
    );
    settings.updateLogo("logo institucional.png", "image/png", new byte[] {1, 2, 3});
    when(institutionSettingsService.getCurrentSettings()).thenReturn(settings);

    var email = templateService.invitation("Ana <Torres>", "https://front.test/reset?token=abc");

    assertThat(email.html()).contains("cid:institution-logo");
    assertThat(email.html()).contains("Av. Los Educadores 123", "Nasca", "Tel. 987 654 321");
    assertThat(email.html()).contains("Ana &lt;Torres&gt;");
    assertThat(email.inlineImages()).singleElement().satisfies(image -> {
      assertThat(image.contentId()).isEqualTo("institution-logo");
      assertThat(image.fileName()).isEqualTo("logo_institucional.png");
      assertThat(image.contentType()).isEqualTo("image/png");
    });
  }

  @Test
  void omitsEmptyOptionalDetailsAndUsesTheFallbackLogo() {
    InstitutionSettings settings = new InstitutionSettings(
        "SIGAE",
        null,
        "   ",
        null,
        "soporte@sigae.edu.pe"
    );
    when(institutionSettingsService.getCurrentSettings()).thenReturn(settings);

    var email = templateService.passwordReset("Gabriel", "https://front.test/reset?token=abc");

    assertThat(email.html()).contains("SIGAE</div>");
    assertThat(email.html()).doesNotContain("cid:institution-logo", "null", "Tel.");
    assertThat(email.html()).contains("soporte@sigae.edu.pe");
    assertThat(email.inlineImages()).isEmpty();
  }
}
