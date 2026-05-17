package com.sigae.api.auth;

import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.security.JwtService;
import com.sigae.api.support.IntegrationTestSupport;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.service.TokenHashingService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private TokenHashingService tokenHashingService;

  @Autowired
  private JwtService jwtService;

  private static final String FORGOT_PASSWORD_SUCCESS_MESSAGE =
      "Si el correo está registrado, recibirás instrucciones de recuperación en los próximos minutos.";

  @Test
  void loginReturnsJwtAndRefreshTokenForActiveUser() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "admin@sigae.edu.pe",
                  "password": "admin123456"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.email").value("admin@sigae.edu.pe"))
        .andExpect(jsonPath("$.user.role").value("Administrador"));
  }

  @Test
  void loginIncludesAssignedLocationIdsForNonAdminUsers() throws Exception {
    var location = createLocation("Biblioteca");
    var user = createUser("Ana Torres", "ana@sigae.edu.pe", "admin123456", UserRole.ENCARGADO, UserStatus.ACTIVE);
    user.setLocations(java.util.Set.of(location));
    userRepository.save(user);

    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "ana@sigae.edu.pe",
                  "password": "admin123456"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.locationIds.length()").value(1))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String accessToken = objectMapper.readTree(response).get("accessToken").asText();
    org.assertj.core.api.Assertions.assertThat(jwtService.parseAccessToken(accessToken).locationIds())
        .containsExactly(location.getId().toString());
  }

  @Test
  void loginFailsForInactiveUser() throws Exception {
    createUser("Ana Torres", "ana@sigae.edu.pe", "admin123456", UserRole.ENCARGADO, UserStatus.INACTIVE);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "ana@sigae.edu.pe",
                  "password": "admin123456"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("La cuenta se encuentra inactiva."));
  }

  @Test
  void forgotPasswordCreatesResetRequestForExistingUser() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);

    mockMvc.perform(post("/api/auth/forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "admin@sigae.edu.pe"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(FORGOT_PASSWORD_SUCCESS_MESSAGE));

    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll()).hasSize(1);
  }

  @Test
  void forgotPasswordReturnsGenericMessageForUnknownUser() throws Exception {
    mockMvc.perform(post("/api/auth/forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "desconocido@sigae.edu.pe"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(FORGOT_PASSWORD_SUCCESS_MESSAGE));

    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll()).isEmpty();
  }

  @Test
  void resetPasswordUpdatesCredentialsAndRevokesRefreshTokens() throws Exception {
    var user = createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String rawToken = "reset-token-123";
    passwordResetRequestRepository.save(new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().plusSeconds(1800)
    ));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "admin@sigae.edu.pe",
                  "password": "admin123456"
                }
                """))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "token": "%s",
                  "newPassword": "NuevaClave1!",
                  "confirmPassword": "NuevaClave1!"
                }
                """.formatted(rawToken)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    org.assertj.core.api.Assertions.assertThat(refreshTokenRepository.findAll())
        .allMatch(refreshToken -> refreshToken.getRevokedAt() != null);
    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll())
        .allMatch(PasswordResetRequest::isUsed);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "admin@sigae.edu.pe",
                  "password": "admin123456"
                }
                """))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "admin@sigae.edu.pe",
                  "password": "NuevaClave1!"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value("admin@sigae.edu.pe"));
  }

  @Test
  void resetPasswordRejectsExpiredToken() throws Exception {
    var user = createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String rawToken = "expired-reset-token";
    passwordResetRequestRepository.save(new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().minusSeconds(60)
    ));

    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "token": "%s",
                  "newPassword": "NuevaClave1!",
                  "confirmPassword": "NuevaClave1!"
                }
                """.formatted(rawToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("El enlace de recuperación es inválido o ya expiró."));
  }

  @Test
  void validateResetPasswordTokenReturnsNoContentForActiveToken() throws Exception {
    var user = createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String rawToken = "valid-reset-token";
    passwordResetRequestRepository.save(new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().plusSeconds(1800)
    ));

    mockMvc.perform(post("/api/auth/reset-password/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "token": "%s"
                }
                """.formatted(rawToken)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    org.assertj.core.api.Assertions.assertThat(passwordResetRequestRepository.findAll())
        .allMatch(resetRequest -> !resetRequest.isUsed());
  }

  @Test
  void validateResetPasswordTokenRejectsExpiredToken() throws Exception {
    var user = createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String rawToken = "expired-validate-token";
    passwordResetRequestRepository.save(new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().minusSeconds(60)
    ));

    mockMvc.perform(post("/api/auth/reset-password/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "token": "%s"
                }
                """.formatted(rawToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("El enlace de recuperación es inválido o ya expiró."));
  }

  @Test
  void validateResetPasswordTokenRejectsUsedToken() throws Exception {
    var user = createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String rawToken = "used-validate-token";
    PasswordResetRequest resetRequest = new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().plusSeconds(1800)
    );
    resetRequest.markUsed();
    passwordResetRequestRepository.save(resetRequest);

    mockMvc.perform(post("/api/auth/reset-password/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "token": "%s"
                }
                """.formatted(rawToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("El enlace de recuperación es inválido o ya expiró."));
  }
}
