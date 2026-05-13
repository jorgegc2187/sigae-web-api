package com.sigae.api.auth;

import com.sigae.api.support.IntegrationTestSupport;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

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
}
