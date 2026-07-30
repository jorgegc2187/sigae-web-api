package com.sigae.api.asset;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssetControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void listInventoryReturnsRequestedPageAndMetadata() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "Laptop Lenovo ThinkPad");
    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-002", "Laptop Lenovo ThinkPad");

    mockMvc.perform(get("/api/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("page", "2")
            .param("size", "1")
            .param("search", "lenovo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].code").value("CMP-2026-001"))
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.hasPrevious").value(true));
  }

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
        .andExpect(jsonPath("$[0].typeId").value(catalog.assetTypeId().toString()))
        .andExpect(jsonPath("$[0].typeName").value("Laptop"))
        .andExpect(jsonPath("$[0].totalUnits").value(2))
        .andExpect(jsonPath("$[0].lastEntryDate").isNotEmpty())
        .andExpect(jsonPath("$[0].units[0].code").value("CMP-2026-001"))
        .andExpect(jsonPath("$[0].units[1].code").value("CMP-2026-002"))
        .andExpect(jsonPath("$[0].units[0].lastInspectionDate").isNotEmpty());
  }

  @Test
  void groupedInventorySeparatesAssetsWithSameNameButDifferentType() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID desktopTypeId = createAssetType(accessToken, catalog.categoryId(), "Desktop", "desktop_windows").typeId();
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-010", "Equipo de Cómputo");
    createAsset(accessToken, desktopTypeId, locationId, "DES-2026-011", "Equipo de Cómputo");

    mockMvc.perform(get("/api/assets/grouped")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].typeName").value(hasItem("Laptop")))
        .andExpect(jsonPath("$[*].typeName").value(hasItem("Desktop")));
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
        .andExpect(jsonPath("$.typeId").value(catalog.assetTypeId().toString()))
        .andExpect(jsonPath("$.typeName").value("Laptop"))
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

    mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Registro agrupado de inventario",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
        """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("CMP-\\d{4}-\\d{3}")))
        .andExpect(jsonPath("$.createdByName").value("Carlos Mendoza"))
        .andExpect(jsonPath("$.decommissionedAt").value(nullValue()));
  }

  @Test
  void assetResponsesExposeCreatedByName() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    String response = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createdByName").value("Carlos Mendoza"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(get("/api/assets/{assetId}", assetId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdByName").value("Carlos Mendoza"));

    mockMvc.perform(get("/api/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].createdByName").value("Carlos Mendoza"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void createDecommissionedAssetPersistsDecommissionedAt() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-055",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Dado de baja",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Activo dado de baja al registrar",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.condition").value("Dado de baja"))
        .andExpect(jsonPath("$.decommissionedAt").isNotEmpty());
  }

  @Test
  void createWithAttachmentPersistsAndDownloadsAssetAttachment() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    byte[] attachmentBytes = "factura-activo".getBytes(StandardCharsets.UTF_8);

    String response = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-050",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Activo con adjunto",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .file(new MockMultipartFile("attachments", "factura.pdf", MediaType.APPLICATION_PDF_VALUE, attachmentBytes))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.attachments.length()").value(1))
        .andExpect(jsonPath("$.attachments[0].fileName").value("factura.pdf"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(response).get("id").asText();
    String attachmentId = objectMapper.readTree(response).get("attachments").get(0).get("id").asText();

    mockMvc.perform(get("/api/assets/{assetId}/attachments/{attachmentId}", assetId, attachmentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_PDF_VALUE)))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
        .andExpect(content().bytes(attachmentBytes));
  }

  @Test
  void updateCanModifyExistingAttributeValueWithoutDuplicatingRows() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    UUID attributeDefinitionId = catalog.attributeDefinitionId();

    String createResponse = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-060",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Activo con atributo dinámico",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Lenovo"
                    }
                  ],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId, attributeDefinitionId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.attributeValues[0].value").value("Lenovo"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/assets/{assetId}", assetId)
            .file(assetPayload("""
                {
                  "code": "CMP-2026-060",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Activo con atributo dinámico actualizado",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Dell"
                    }
                  ],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId, attributeDefinitionId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attributeValues.length()").value(1))
        .andExpect(jsonPath("$.attributeValues[0].value").value("Dell"));
  }

  @Test
  void updateClearsDecommissionedAtAndRegistersReactivatedTraceability() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    String createResponse = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-070",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Dado de baja",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Activo inactivo",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.decommissionedAt").isNotEmpty())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/assets/{assetId}", assetId)
            .file(assetPayload("""
                {
                  "code": "CMP-2026-070",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Activo reactivado",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.condition").value("Bueno"))
        .andExpect(jsonPath("$.decommissionedAt").value(nullValue()));

    mockMvc.perform(get("/api/assets/{assetId}/traceability", assetId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].eventType").value(hasItem("REACTIVATED")));
  }

  @Test
  void updateRegistersDetailedTraceabilityWithAuthenticatedUser() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    UUID attributeDefinitionId = catalog.attributeDefinitionId();

    String createResponse = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-080",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "notes": "Activo inicial",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Lenovo"
                    }
                  ],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId, attributeDefinitionId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/assets/{assetId}", assetId)
            .file(assetPayload("""
                {
                  "code": "CMP-2026-080",
                  "name": "Laptop Dell Latitude",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Regular",
                  "notes": "Activo actualizado por mantenimiento",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Dell"
                    }
                  ],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId, attributeDefinitionId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());

    var traceability = objectMapper.readTree(
        mockMvc.perform(get("/api/assets/{assetId}/traceability", assetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
    );

    boolean hasGenericUpdated = false;
    boolean hasNameChange = false;
    boolean hasConditionChange = false;
    boolean hasAttributeChange = false;

    for (var node : traceability) {
      String description = node.get("description").asText();
      if ("Activo actualizado.".equals(description)) {
        hasGenericUpdated = true;
      }

      if ("Nombre del activo actualizado.".equals(description)
          && "Laptop Lenovo ThinkPad".equals(node.path("previousValue").asText())
          && "Laptop Dell Latitude".equals(node.path("newValue").asText())
          && "Carlos Mendoza".equals(node.path("userName").asText())) {
        hasNameChange = true;
      }

      if ("CONDITION_CHANGED".equals(node.path("eventType").asText())
          && "Bueno".equals(node.path("previousValue").asText())
          && "Regular".equals(node.path("newValue").asText())
          && "Carlos Mendoza".equals(node.path("userName").asText())) {
        hasConditionChange = true;
      }

      if ("Atributo \"Marca\" actualizado.".equals(description)
          && "Lenovo".equals(node.path("previousValue").asText())
          && "Dell".equals(node.path("newValue").asText())
          && "Carlos Mendoza".equals(node.path("userName").asText())) {
        hasAttributeChange = true;
      }
    }

    Assertions.assertFalse(hasGenericUpdated);
    Assertions.assertTrue(hasNameChange);
    Assertions.assertTrue(hasConditionChange);
    Assertions.assertTrue(hasAttributeChange);
  }

  @Test
  void changeStatusStoresReasonAndEvidenceInTraceability() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    byte[] evidenceBytes = "pdf-evidencia".getBytes(StandardCharsets.UTF_8);

    String createResponse = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-090",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "notes": "Activo operativo",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(multipart("/api/assets/{assetId}/status-change", assetId)
            .file(assetStatusPayload("""
                {
                  "nextCondition": "DADO_DE_BAJA",
                  "reason": "Daño estructural irreversible"
                }
                """))
            .file(new MockMultipartFile("attachments", "evidencia.pdf", MediaType.APPLICATION_PDF_VALUE, evidenceBytes))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.condition").value("Dado de baja"))
        .andExpect(jsonPath("$.decommissionedAt").isNotEmpty());

    String traceabilityResponse = mockMvc.perform(get("/api/assets/{assetId}/traceability", assetId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].eventType").value("DECOMMISSIONED"))
        .andExpect(jsonPath("$[0].reason").value("Daño estructural irreversible"))
        .andExpect(jsonPath("$[0].attachments", hasSize(1)))
        .andExpect(jsonPath("$[0].attachments[0].fileName").value("evidencia.pdf"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String attachmentId = objectMapper.readTree(traceabilityResponse).get(0).get("attachments").get(0).get("id").asText();
    String traceabilityId = objectMapper.readTree(traceabilityResponse).get(0).get("id").asText();

    mockMvc.perform(get("/api/assets/{assetId}/traceability/{traceabilityId}/attachments/{attachmentId}", assetId, traceabilityId, attachmentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_PDF_VALUE)))
        .andExpect(content().bytes(evidenceBytes));
  }

  @Test
  void changeStatusRejectsEvidenceLargerThanFiveMb() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");

    String createResponse = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "CMP-2026-091",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "notes": "Activo operativo",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(catalog.assetTypeId(), locationId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String assetId = objectMapper.readTree(createResponse).get("id").asText();
    byte[] oversizedEvidence = new byte[6 * 1024 * 1024];

    mockMvc.perform(multipart("/api/assets/{assetId}/status-change", assetId)
            .file(assetStatusPayload("""
                {
                  "nextCondition": "MANTENIMIENTO",
                  "reason": "Requiere evaluación técnica"
                }
                """))
            .file(new MockMultipartFile("attachments", "evidencia.pdf", MediaType.APPLICATION_PDF_VALUE, oversizedEvidence))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isBadRequest());
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

    AssetTypeFixture type = createAssetType(accessToken, categoryId, "Laptop", "laptop_mac");
    return new AssetCatalog(
        categoryId,
        type.typeId(),
        type.attributeDefinitionId()
    );
  }

  private AssetTypeFixture createAssetType(String accessToken, UUID categoryId, String name, String icon) throws Exception {
    String typeResponse = mockMvc.perform(post("/api/categories/%s/types".formatted(categoryId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "icon": "%s",
                  "attributes": [
                    {
                      "name": "Marca",
                      "description": "Fabricante del equipo",
                      "isRequired": true
                    }
                  ]
                }
                """.formatted(name, icon)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    var typeNode = objectMapper.readTree(typeResponse);
    return new AssetTypeFixture(
        UUID.fromString(typeNode.get("id").asText()),
        UUID.fromString(typeNode.get("attributes").get(0).get("id").asText())
    );
  }

  private void createAsset(
      String accessToken,
      UUID assetTypeId,
      UUID locationId,
      String code,
      String name
  ) throws Exception {
    mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "%s",
                  "name": "%s",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "Bueno",
                  "serialNumber": "%s-SN",
                  "acquisitionDate": "2026-01-15",
                  "notes": "Registro agrupado de inventario",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(code, name, assetTypeId, locationId, code)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated());
  }

  private MockMultipartFile assetPayload(String content) {
    return new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        content.getBytes(StandardCharsets.UTF_8)
    );
  }

  private MockMultipartFile assetStatusPayload(String content) {
    return new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        content.getBytes(StandardCharsets.UTF_8)
    );
  }

  private record AssetCatalog(UUID categoryId, UUID assetTypeId, UUID attributeDefinitionId) {}

  private record AssetTypeFixture(UUID typeId, UUID attributeDefinitionId) {}
}
