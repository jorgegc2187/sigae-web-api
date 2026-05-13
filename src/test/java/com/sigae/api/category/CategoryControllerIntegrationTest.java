package com.sigae.api.category;

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

class CategoryControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void adminCanCreateCategoryWithTypesLater() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(post("/api/categories")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Tecnología",
                  "icon": "devices"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Tecnología"))
        .andExpect(jsonPath("$.icon").value("devices"))
        .andExpect(jsonPath("$.typesCount").value(0))
        .andExpect(jsonPath("$.assetsCount").value(0));
  }

  @Test
  void authenticatedUserCanReadCategories() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");

    mockMvc.perform(get("/api/categories")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());
  }
}
