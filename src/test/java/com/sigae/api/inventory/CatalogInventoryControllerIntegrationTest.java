package com.sigae.api.inventory;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogInventoryControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void adminCanCreateAndReadBasicCatalogs() throws Exception {
    String accessToken = createAdminAndLogin();

    mockMvc.perform(post("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Laboratorio de Cómputo",
                  "description": "Aula equipada para clases de tecnología.",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Laboratorio de Cómputo"))
        .andExpect(jsonPath("$.status").value("Activo"));

    mockMvc.perform(post("/api/suppliers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "TecnoEdu Perú",
                  "ruc": "20604578912",
                  "email": "ventas@tecnoedu.pe",
                  "phone": "999000111",
                  "address": "Lima",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("TecnoEdu Perú"));

    mockMvc.perform(post("/api/teachers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "dni": "45678912",
                  "fullName": "María Torres",
                  "specialty": "Ciencias",
                  "email": "maria.torres@sigae.edu.pe",
                  "phone": "988777666",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fullName").value("María Torres"));

    mockMvc.perform(get("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Laboratorio de Cómputo"));
  }

  @Test
  void encargadoCanCreateAssetAndReadTraceability() throws Exception {
    String adminToken = createAdminAndLogin();
    UUID assetTypeId = createAssetType(adminToken);
    UUID attributeDefinitionId = getFirstAttributeDefinitionId(adminToken);
    UUID locationId = createLocation(adminToken);
    UUID supplierId = createSupplier(adminToken);

    createUser("Luis Quispe", "luis@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    String encargadoToken = loginAndGetAccessToken("luis@sigae.edu.pe", "encargado123");

    String assetResponse = mockMvc.perform(post("/api/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + encargadoToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "CMP-2026-001",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "supplierId": "%s",
                  "condition": "Bueno",
                  "serialNumber": "LNV-T14-001",
                  "barcode": "BC-CMP-2026-001",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Registro inicial",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Lenovo"
                    }
                  ]
                }
                """.formatted(assetTypeId, locationId, supplierId, attributeDefinitionId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("CMP-2026-001"))
        .andExpect(jsonPath("$.condition").value("Bueno"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    UUID assetId = UUID.fromString(objectMapper.readTree(assetResponse).get("id").asText());

    mockMvc.perform(get("/api/assets/%s/traceability".formatted(assetId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + encargadoToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].eventType").value("CREATED"));
  }

  @Test
  void soloLecturaCannotMutateInventory() throws Exception {
    createUser("Ana Lectura", "lectura@sigae.edu.pe", "lectura123", UserRole.SOLO_LECTURA, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("lectura@sigae.edu.pe", "lectura123");

    mockMvc.perform(post("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Aula 101",
                  "description": "Aula regular",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanDeactivateSupplier() throws Exception {
    String accessToken = createAdminAndLogin();
    UUID supplierId = createSupplier(accessToken);

    mockMvc.perform(patch("/api/suppliers/%s/deactivate".formatted(supplierId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Inactivo"));
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
  }

  private UUID createLocation(String accessToken) throws Exception {
    String response = mockMvc.perform(post("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Laboratorio de Cómputo",
                  "description": "Aula equipada para clases de tecnología.",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private UUID createSupplier(String accessToken) throws Exception {
    String response = mockMvc.perform(post("/api/suppliers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "TecnoEdu Perú",
                  "ruc": "20604578912",
                  "email": "ventas@tecnoedu.pe",
                  "phone": "999000111",
                  "address": "Lima",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private UUID createAssetType(String accessToken) throws Exception {
    String categoryResponse = mockMvc.perform(post("/api/categories")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Tecnología",
                  "icon": "devices"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    UUID categoryId = UUID.fromString(objectMapper.readTree(categoryResponse).get("id").asText());

    String typeResponse = mockMvc.perform(post("/api/categories/%s/types".formatted(categoryId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Laptop",
                  "icon": "laptop_mac",
                  "attributes": [
                    {
                      "name": "Marca",
                      "description": "Fabricante del equipo",
                      "isRequired": true
                    }
                  ]
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(typeResponse).get("id").asText());
  }

  private UUID getFirstAttributeDefinitionId(String accessToken) throws Exception {
    String response = mockMvc.perform(get("/api/categories")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get(0).get("types").get(0).get("attributes").get(0).get("id").asText());
  }
}
