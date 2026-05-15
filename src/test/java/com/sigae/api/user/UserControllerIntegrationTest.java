package com.sigae.api.user;

import com.sigae.api.exception.MailDeliveryException;
import com.sigae.api.service.UserInvitationMailService;
import com.sigae.api.support.IntegrationTestSupport;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends IntegrationTestSupport {

  @MockitoBean
  UserInvitationMailService userInvitationMailService;

  @Test
  void adminCanCreateUser() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Luis Quispe",
                  "email": "luis@sigae.edu.pe",
                  "password": "encargado123",
                  "role": "Encargado",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("luis@sigae.edu.pe"))
        .andExpect(jsonPath("$.role").value("Encargado"));
  }

  @Test
  void adminCanCreateUserWithInvitation() throws Exception {
    doNothing().when(userInvitationMailService).sendInvitationMail(any(), anyString());
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Ana Torres",
                  "email": "ana@sigae.edu.pe",
                  "role": "Encargado",
                  "status": "Activo",
                  "sendInvitation": true
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("ana@sigae.edu.pe"))
        .andExpect(jsonPath("$.role").value("Encargado"));

    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll()).hasSize(1);
  }

  @Test
  void adminGetsServiceUnavailableWhenInvitationMailFails() throws Exception {
    doThrow(new MailDeliveryException(
        "No se pudo enviar el correo de invitación. Verifique la configuración SMTP e intente nuevamente.",
        new RuntimeException("SMTP test failure")
    )).when(userInvitationMailService).sendInvitationMail(any(), anyString());
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Paula Ortiz",
                  "email": "paula@sigae.edu.pe",
                  "role": "Encargado",
                  "status": "Activo",
                  "sendInvitation": true
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value(
            "No se pudo enviar el correo de invitación. Verifique la configuración SMTP e intente nuevamente."
        ));

    org.assertj.core.api.Assertions.assertThat(userRepository.findByEmailIgnoreCase("paula@sigae.edu.pe")).isEmpty();
    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll()).isEmpty();
  }

  @Test
  void nonAdminCannotListUsers() throws Exception {
    createUser("Luis Quispe", "luis@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("luis@sigae.edu.pe", "encargado123");

    mockMvc.perform(get("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }
}
