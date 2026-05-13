package com.sigae.api.user;

import com.sigae.api.support.IntegrationTestSupport;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends IntegrationTestSupport {

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
  void nonAdminCannotListUsers() throws Exception {
    createUser("Luis Quispe", "luis@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("luis@sigae.edu.pe", "encargado123");

    mockMvc.perform(get("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }
}
