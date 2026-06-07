package com.sigae.api.location;

import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocationControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void adminCanCreateLocationWithManagers() throws Exception {
    String accessToken = createAdminAndLogin();
    User manager = createUser("Luis Encargado", "luis.manager@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);

    String response = mockMvc.perform(post("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Laboratorio Norte",
                  "description": "Aula equipada para tecnología.",
                  "status": "Activo",
                  "managerIds": ["%s"]
                }
                """.formatted(manager.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Laboratorio Norte"))
        .andExpect(jsonPath("$.managers.length()").value(1))
        .andExpect(jsonPath("$.managers[0].fullName").value("Luis Encargado"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    UUID locationId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
    User reloadedManager = userRepository.findById(manager.getId()).orElseThrow();

    assertThat(reloadedManager.getLocations())
        .extracting(Location::getId)
        .contains(locationId);
  }

  @Test
  void adminCanUpdateManagersAndDeactivateLocation() throws Exception {
    String accessToken = createAdminAndLogin();
    User previousManager = createUser("Ana Encargada", "ana.manager@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    User nextManager = createUser("Pedro Encargado", "pedro.manager@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    Location location = createLocation("Biblioteca Central");

    previousManager.setLocations(Set.of(location));
    userRepository.save(previousManager);

    mockMvc.perform(patch("/api/locations/%s".formatted(location.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Biblioteca Central",
                  "description": "Ubicación actualizada",
                  "status": "Activo",
                  "managerIds": ["%s"]
                }
                """.formatted(nextManager.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Ubicación actualizada"))
        .andExpect(jsonPath("$.managers.length()").value(1))
        .andExpect(jsonPath("$.managers[0].fullName").value("Pedro Encargado"));

    mockMvc.perform(patch("/api/locations/%s/status".formatted(location.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "Inactivo"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Inactivo"));

    User reloadedPreviousManager = userRepository.findById(previousManager.getId()).orElseThrow();
    User reloadedNextManager = userRepository.findById(nextManager.getId()).orElseThrow();

    assertThat(reloadedPreviousManager.getLocations())
        .extracting(Location::getId)
        .doesNotContain(location.getId());
    assertThat(reloadedNextManager.getLocations())
        .extracting(Location::getId)
        .contains(location.getId());
  }

  @Test
  void listCanFilterOnlyActiveLocations() throws Exception {
    String accessToken = createAdminAndLogin();
    createLocation("Aula Activa");
    Location inactiveLocation = createLocation("Aula Inactiva");
    inactiveLocation.setStatus(com.sigae.api.model.entity.CatalogStatus.INACTIVE);
    locationRepository.save(inactiveLocation);

    mockMvc.perform(get("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Aula Activa"));
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Carlos Mendoza", "admin.locations@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin.locations@sigae.edu.pe", "admin123456");
  }
}
