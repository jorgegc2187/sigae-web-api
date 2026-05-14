package com.sigae.api.reports;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void adminCanListAssetReportWithFilters() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken);
    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "2026-01-15");

    mockMvc.perform(get("/api/reports/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("categoryId", catalog.categoryId().toString())
            .param("locationId", locationId.toString())
            .param("startDate", "2026-01-01")
            .param("endDate", "2026-01-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("CMP-2026-001"))
        .andExpect(jsonPath("$[0].category").value("Tecnología"))
        .andExpect(jsonPath("$[0].location").value("Laboratorio de Cómputo"));
  }

  @Test
  void soloLecturaCanExportAssetReports() throws Exception {
    String adminToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(adminToken);
    UUID locationId = createLocation(adminToken);
    createAsset(adminToken, catalog.assetTypeId(), locationId, "CMP-2026-002", "2026-02-15");

    createUser("Ana Lectura", "lectura@sigae.edu.pe", "lectura123", UserRole.SOLO_LECTURA, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("lectura@sigae.edu.pe", "lectura123");

    assertExport(accessToken, "pdf", "application/pdf", ".pdf");
    assertExport(accessToken, "excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
    assertExport(accessToken, "word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
  }

  @Test
  void reportsRejectAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/reports/assets"))
        .andExpect(status().isUnauthorized());
  }

  private void assertExport(
      String accessToken,
      String format,
      String contentType,
      String extension
  ) throws Exception {
    mockMvc.perform(get("/api/reports/assets/export")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("format", format))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(contentType)))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(extension)));
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

  private AssetCatalog createAssetCatalog(String accessToken) throws Exception {
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

    return new AssetCatalog(categoryId, UUID.fromString(objectMapper.readTree(typeResponse).get("id").asText()));
  }

  private void createAsset(
      String accessToken,
      UUID assetTypeId,
      UUID locationId,
      String code,
      String acquisitionDate
  ) throws Exception {
    mockMvc.perform(post("/api/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "%s",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "serialNumber": "LNV-T14-001",
                  "barcode": "BC-%s",
                  "acquisitionDate": "%s",
                  "notes": "Registro para reporte",
                  "attributeValues": []
                }
                """.formatted(code, assetTypeId, locationId, code, acquisitionDate)))
        .andExpect(status().isCreated());
  }

  private record AssetCatalog(UUID categoryId, UUID assetTypeId) {}
}
