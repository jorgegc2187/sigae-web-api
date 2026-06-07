package com.sigae.api.asset;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssetControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void groupedInventoryReturnsAssetFamiliesWithUnits() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    UUID secondaryLocationId = createLocation(accessToken, "Biblioteca");

    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "Laptop Lenovo ThinkPad");
    createAsset(accessToken, catalog.assetTypeId(), secondaryLocationId, "CMP-2026-002", "Laptop Lenovo ThinkPad");
    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-003", "Laptop Dell Latitude");

    mockMvc.perform(get("/api/assets/grouped")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("search", "lenovo")
            .param("categoryId", catalog.categoryId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].displayName").value("Laptop Lenovo ThinkPad"))
        .andExpect(jsonPath("$[0].categoryId").value(catalog.categoryId().toString()))
        .andExpect(jsonPath("$[0].categoryIcon").value("devices"))
        .andExpect(jsonPath("$[0].categoryName").value("Tecnología"))
        .andExpect(jsonPath("$[0].totalUnits").value(2))
        .andExpect(jsonPath("$[0].lastEntryDate").isNotEmpty())
        .andExpect(jsonPath("$[0].units[0].code").value("CMP-2026-001"))
        .andExpect(jsonPath("$[0].units[1].code").value("CMP-2026-002"))
        .andExpect(jsonPath("$[0].units[0].lastInspectionDate").isNotEmpty());
  }

  @Test
  void groupedInventoryDetailReturnsSingleFamilyByGroupId() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "Laptop Lenovo ThinkPad");
    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-002", "Laptop Lenovo ThinkPad");

    String groupedResponse = mockMvc.perform(get("/api/assets/grouped")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String groupId = objectMapper.readTree(groupedResponse).get(0).get("groupId").asText();

    mockMvc.perform(get("/api/assets/grouped/{groupId}", groupId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupId").value(groupId))
        .andExpect(jsonPath("$.displayName").value("Laptop Lenovo ThinkPad"))
        .andExpect(jsonPath("$.categoryId").value(catalog.categoryId().toString()))
        .andExpect(jsonPath("$.categoryIcon").value("devices"))
        .andExpect(jsonPath("$.totalUnits").value(2))
        .andExpect(jsonPath("$.lastEntryDate").isNotEmpty())
        .andExpect(jsonPath("$.units.length()").value(2));
  }

  @Test
  void lookupReturnsAssetByCode() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "Laptop Lenovo ThinkPad");

    mockMvc.perform(get("/api/assets/lookup")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("value", "cmp-2026-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("CMP-2026-001"))
        .andExpect(jsonPath("$.name").value("Laptop Lenovo ThinkPad"));
  }

  @Test
  void lookupReturnsAssetByBarcode() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "Laptop Lenovo ThinkPad");

    mockMvc.perform(get("/api/assets/lookup")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("value", "bc-cmp-2026-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("CMP-2026-001"))
        .andExpect(jsonPath("$.barcode").value("BC-CMP-2026-001"));
  }

  @Test
  void lookupReturnsNotFoundWhenAssetDoesNotExist() throws Exception {
    String accessToken = createAdminAndLogin();

    mockMvc.perform(get("/api/assets/lookup")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("value", "NO-EXISTE"))
        .andExpect(status().isNotFound());
  }

  @Test
  void createWithoutCodeGeneratesOperationalCode() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    mockMvc.perform(post("/api/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Registro agrupado de inventario",
                  "attributeValues": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("CMP-\\d{4}-\\d{3}")))
        .andExpect(jsonPath("$.barcode").isNotEmpty());
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
  }

  private UUID createLocation(String accessToken, String name) throws Exception {
    String response = mockMvc.perform(post("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "description": "Ubicación para activos de inventario.",
                  "status": "Activo"
                }
                """.formatted(name)))
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
      String name
  ) throws Exception {
    mockMvc.perform(post("/api/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "%s",
                  "name": "%s",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "serialNumber": "%s-SN",
                  "barcode": "BC-%s",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Registro agrupado de inventario",
                  "attributeValues": []
                }
                """.formatted(code, name, assetTypeId, locationId, code, code)))
        .andExpect(status().isCreated());
  }

  private record AssetCatalog(UUID categoryId, UUID assetTypeId) {}
}
