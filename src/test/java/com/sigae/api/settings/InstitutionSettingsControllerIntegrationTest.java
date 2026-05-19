package com.sigae.api.settings;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstitutionSettingsControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void brandingIsPublicAndReturnsDefaultValues() throws Exception {
    mockMvc.perform(get("/api/settings/branding"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.systemName").value("SIGAE"))
        .andExpect(jsonPath("$.hasLogo").value(false));
  }

  @Test
  void fullSettingsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/settings"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminCanUpdateInstitutionSettingsAndLogo() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    MockMultipartFile payload = new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        """
            {
              "systemName": "I.E. Simón Rodríguez - Nasca",
              "address": "Av. Principal 123",
              "city": "Nasca",
              "supportPhone": "+51 999 999 999",
              "supportEmail": "contacto@colegio.edu.pe"
            }
            """.getBytes()
    );
    MockMultipartFile logo = new MockMultipartFile(
        "logo",
        "logo.png",
        MediaType.IMAGE_PNG_VALUE,
        new byte[] {(byte) 137, 80, 78, 71}
    );

    mockMvc.perform(multipart("/api/settings")
            .file(payload)
            .file(logo)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .with(csrf())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.systemName").value("I.E. Simón Rodríguez - Nasca"))
        .andExpect(jsonPath("$.address").value("Av. Principal 123"))
        .andExpect(jsonPath("$.city").value("Nasca"))
        .andExpect(jsonPath("$.supportPhone").value("+51 999 999 999"))
        .andExpect(jsonPath("$.supportEmail").value("contacto@colegio.edu.pe"))
        .andExpect(jsonPath("$.hasLogo").value(true));

    mockMvc.perform(get("/api/settings/branding"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.systemName").value("I.E. Simón Rodríguez - Nasca"))
        .andExpect(jsonPath("$.hasLogo").value(true));

    mockMvc.perform(get("/api/settings/logo"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("inline")))
        .andExpect(content().bytes(new byte[] {(byte) 137, 80, 78, 71}));
  }

  @Test
  void nonAdminCannotUpdateInstitutionSettings() throws Exception {
    createUser("Lucía Pérez", "lectura@sigae.edu.pe", "lectura123", UserRole.SOLO_LECTURA, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("lectura@sigae.edu.pe", "lectura123");

    MockMultipartFile payload = new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        """
            {
              "systemName": "SIGAE",
              "address": "",
              "city": "",
              "supportPhone": "",
              "supportEmail": "contacto@institucion.edu.pe"
            }
            """.getBytes()
    );

    mockMvc.perform(multipart("/api/settings")
            .file(payload)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .with(csrf())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }
}
