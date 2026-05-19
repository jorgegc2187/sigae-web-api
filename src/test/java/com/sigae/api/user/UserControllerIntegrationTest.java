package com.sigae.api.user;

import com.sigae.api.exception.MailDeliveryException;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.PasswordResetRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends IntegrationTestSupport {

  @MockitoBean
  UserInvitationMailService userInvitationMailService;

  @Test
  void adminCanCreateUser() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    Location library = createLocation("Biblioteca");
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
                  "locationIds": ["%s", "%s"]
                }
                """.formatted(lab.getId(), library.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("luis@sigae.edu.pe"))
        .andExpect(jsonPath("$.role").value("Encargado"))
        .andExpect(jsonPath("$.locationIds.length()").value(2))
        .andExpect(jsonPath("$.locationNames.length()").value(2));
  }

  @Test
  void adminCanCreateUserWithInvitation() throws Exception {
    doNothing().when(userInvitationMailService).sendInvitationMail(any(), anyString());
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Ana Torres",
                  "email": "ana@sigae.edu.pe",
                  "role": "Encargado",
                  "locationIds": ["%s"],
                  "sendInvitation": true
                }
                """.formatted(lab.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("ana@sigae.edu.pe"))
        .andExpect(jsonPath("$.role").value("Encargado"))
        .andExpect(jsonPath("$.status").value("Pendiente"))
        .andExpect(jsonPath("$.invitationStatus").value("ACTIVE"));

    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll()).hasSize(1);
  }

  @Test
  void adminGetsServiceUnavailableWhenInvitationMailFails() throws Exception {
    doThrow(new MailDeliveryException(
        "No se pudo enviar el correo de invitación. Verifique la configuración SMTP e intente nuevamente.",
        new RuntimeException("SMTP test failure")
    )).when(userInvitationMailService).sendInvitationMail(any(), anyString());
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Paula Ortiz",
                  "email": "paula@sigae.edu.pe",
                  "role": "Encargado",
                  "locationIds": ["%s"],
                  "sendInvitation": true
                }
                """.formatted(lab.getId())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value(
            "No se pudo enviar el correo de invitación. Verifique la configuración SMTP e intente nuevamente."
        ));

    org.assertj.core.api.Assertions.assertThat(userRepository.findByEmailIgnoreCase("paula@sigae.edu.pe")).isEmpty();
    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll()).isEmpty();
  }

  @Test
  void adminCanCreateAdministratorWithoutAssignedLocations() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "María Flores",
                  "email": "maria@sigae.edu.pe",
                  "password": "AdminTemp1!",
                  "role": "Administrador",
                  "locationIds": ["%s"]
                }
                """.formatted(lab.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("Administrador"))
        .andExpect(jsonPath("$.locationIds.length()").value(0))
        .andExpect(jsonPath("$.locationNames.length()").value(0));
  }

  @Test
  void nonAdminRequiresAssignedLocations() throws Exception {
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
                  "locationIds": []
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Debe asignar al menos una ubicación para este rol."));
  }

  @Test
  void nonAdminCannotListUsers() throws Exception {
    createUser("Luis Quispe", "luis@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("luis@sigae.edu.pe", "encargado123");

    mockMvc.perform(get("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanDeactivateAnotherUser() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    createUser("Ana Torres", "ana@sigae.edu.pe", "admin123456", UserRole.ENCARGADO, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
    String targetUserId = userRepository.findByEmailIgnoreCase("ana@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(patch("/api/users/{userId}/status", targetUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Inactivo"));
  }

  @Test
  void adminCannotDeactivateSelf() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
    String currentUserId = userRepository.findByEmailIgnoreCase("admin@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(patch("/api/users/{userId}/status", currentUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No puede desactivarse a sí mismo."));
  }

  @Test
  void adminCannotDeactivateLastActiveAdministrator() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    createUser("Laura Ruiz", "laura@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.INACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
    String currentUserId = userRepository.findByEmailIgnoreCase("admin@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(patch("/api/users/{userId}/status", currentUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No puede desactivarse a sí mismo."));
  }

  @Test
  void adminCannotDemoteLastActiveAdministrator() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    createUser("Laura Ruiz", "laura@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.INACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
    String currentUserId = userRepository.findByEmailIgnoreCase("admin@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(patch("/api/users/{userId}", currentUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Carlos Mendoza",
                  "email": "admin@sigae.edu.pe",
                  "role": "ENCARGADO",
                  "locationIds": ["%s"]
                }
                """.formatted(lab.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No puede quitarse a sí mismo el rol de administrador."));
  }

  @Test
  void pendingUsersCannotBeUpdatedThroughStatusToggle() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    createUser("Ana Torres", "ana@sigae.edu.pe", "admin123456", UserRole.ENCARGADO, UserStatus.PENDING);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
    String targetUserId = userRepository.findByEmailIgnoreCase("ana@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(patch("/api/users/{userId}/status", targetUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "ACTIVE"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Los usuarios pendientes no pueden activarse o desactivarse desde esta sección."));
  }

  @Test
  void listShowsExpiredInvitationStatusForPendingUser() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    var pendingUser = createUser("Ana Torres", "ana@sigae.edu.pe", "admin123456", UserRole.ENCARGADO, UserStatus.PENDING);
    passwordResetRequestRepository.save(new PasswordResetRequest(
        pendingUser,
        "expired-invitation-hash",
        java.time.Instant.now().minusSeconds(60),
        PasswordResetPurpose.ACCOUNT_SETUP
    ));
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(get("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.email=='ana@sigae.edu.pe')].invitationStatus").value(org.hamcrest.Matchers.contains("EXPIRED")));
  }

  @Test
  void adminCanCancelActiveInvitation() throws Exception {
    doNothing().when(userInvitationMailService).sendInvitationMail(any(), anyString());
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Ana Torres",
                  "email": "ana@sigae.edu.pe",
                  "role": "Encargado",
                  "locationIds": ["%s"],
                  "sendInvitation": true
                }
                """.formatted(lab.getId())))
        .andExpect(status().isCreated());

    String targetUserId = userRepository.findByEmailIgnoreCase("ana@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(post("/api/users/{userId}/invitation/cancel", targetUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invitationStatus").value("CANCELLED"));
  }

  @Test
  void adminCanResendInvitationAndInvalidatesPreviousOne() throws Exception {
    doNothing().when(userInvitationMailService).sendInvitationMail(any(), anyString());
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    Location lab = createLocation("Aula de Cómputo");
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "Ana Torres",
                  "email": "ana@sigae.edu.pe",
                  "role": "Encargado",
                  "locationIds": ["%s"],
                  "sendInvitation": true
                }
                """.formatted(lab.getId())))
        .andExpect(status().isCreated());

    String targetUserId = userRepository.findByEmailIgnoreCase("ana@sigae.edu.pe").orElseThrow().getId().toString();

    mockMvc.perform(post("/api/users/{userId}/invitation/resend", targetUserId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invitationStatus").value("ACTIVE"));

    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll())
        .hasSize(2)
        .filteredOn(request -> request.getPurpose() == PasswordResetPurpose.ACCOUNT_SETUP)
        .hasSize(2);
    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll())
        .filteredOn(request -> request.getPurpose() == PasswordResetPurpose.ACCOUNT_SETUP && request.getCancelledAt() != null)
        .hasSize(1);
  }
}
