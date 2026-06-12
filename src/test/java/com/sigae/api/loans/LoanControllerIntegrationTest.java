package com.sigae.api.loans;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerIntegrationTest extends IntegrationTestSupport {

  @Test
  void createLoanWithoutAttachmentsReturnsCreatedLoan() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-001", "Laptop Lenovo ThinkPad");

    MockMultipartFile payload = new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        """
            {
              "teacherId": "%s",
              "destinationLocationId": "%s",
              "loanDate": "2026-05-14",
              "dueDate": "2026-05-20",
              "notes": "Préstamo de prueba",
              "assetIds": ["%s"],
              "attachmentSources": []
            }
            """.formatted(teacher.id(), locationId, assetId).getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/loans")
            .file(payload)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(startsWith("PRE-2026-")))
        .andExpect(jsonPath("$.teacher.name").value("Alejandro Cárdenas"))
        .andExpect(jsonPath("$.createdByName").value("Carlos Mendoza"))
        .andExpect(jsonPath("$.assets.length()").value(1))
        .andExpect(jsonPath("$.activities[0].title").value("Préstamo registrado"))
        .andExpect(jsonPath("$.activities[0].actor").value("Carlos Mendoza"))
        .andExpect(jsonPath("$.attachments.length()").value(0))
        .andExpect(jsonPath("$.signatureDataUrl").doesNotExist());
  }

  @Test
  void createLoanWithSignatureAndAttachmentPersistsBinaryContent() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Cómputo");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-002", "Laptop Dell Latitude");

    MockMultipartFile payload = new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        """
            {
              "teacherId": "%s",
              "destinationLocationId": "%s",
              "loanDate": "2026-05-14",
              "dueDate": "2026-05-21",
              "notes": "Préstamo con firma y adjunto",
              "assetIds": ["%s"],
              "attachmentSources": ["picker"]
            }
            """.formatted(teacher.id(), locationId, assetId).getBytes(StandardCharsets.UTF_8)
    );
    MockMultipartFile signature = new MockMultipartFile(
        "signature",
        "firma-prestamo.png",
        MediaType.IMAGE_PNG_VALUE,
        new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}
    );
    byte[] attachmentBytes = "adjunto-prestamo".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile attachment = new MockMultipartFile(
        "attachments",
        "acta-entrega.pdf",
        MediaType.APPLICATION_PDF_VALUE,
        attachmentBytes
    );

    String response = mockMvc.perform(multipart("/api/loans")
            .file(payload)
            .file(signature)
            .file(attachment)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.attachments.length()").value(1))
        .andExpect(jsonPath("$.attachments[0].fileName").value("acta-entrega.pdf"))
        .andExpect(jsonPath("$.attachments[0].mimeType").value(MediaType.APPLICATION_PDF_VALUE))
        .andExpect(jsonPath("$.attachments[0].sizeBytes").value(attachmentBytes.length))
        .andExpect(jsonPath("$.attachments[0].source").value("picker"))
        .andExpect(jsonPath("$.signatureDataUrl").value(startsWith("data:image/png;base64,")))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String loanId = objectMapper.readTree(response).get("id").asText();
    String attachmentId = objectMapper.readTree(response).get("attachments").get(0).get("id").asText();

    mockMvc.perform(get("/api/loans/{loanId}/attachments/{attachmentId}", loanId, attachmentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_PDF_VALUE)))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
        .andExpect(content().bytes(attachmentBytes));
  }

  @Test
  void returnLoanWithoutBodyKeepsCompatibility() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Laboratorio de Robótica");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-011", "Laptop HP ProBook");
    UUID loanId = createLoan(accessToken, teacher.id(), locationId, assetId);

    mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Devuelto"))
        .andExpect(jsonPath("$.assets[0].status").value("Bueno"))
        .andExpect(jsonPath("$.activities[0].title").value("Préstamo devuelto"))
        .andExpect(jsonPath("$.activities[0].actor").value("Carlos Mendoza"));
  }

  @Test
  void returnLoanWithIncidentUpdatesAssetConditionAndTraceability() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Sala de Innovación");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-012", "Proyector Epson");
    UUID loanId = createLoan(accessToken, teacher.id(), locationId, assetId);

    mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "assetReviews": [
                    {
                      "assetId": "%s",
                      "hasIncident": true,
                      "incidentDescription": "Lente con fisura visible",
                      "conditionAfterReturn": "Malo"
                    }
                  ]
                }
                """.formatted(assetId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Devuelto"))
        .andExpect(jsonPath("$.assets[0].status").value("Malo"))
        .andExpect(jsonPath("$.activities[0].actor").value("Carlos Mendoza"))
        .andExpect(jsonPath("$.activities[*].title").value(hasItem("Incidencia registrada")));

    mockMvc.perform(get("/api/assets/{assetId}", assetId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.condition").value("Malo"));

    mockMvc.perform(get("/api/assets/{assetId}/traceability", assetId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].eventType").value(hasItem("RETURNED")))
        .andExpect(jsonPath("$[*].eventType").value(hasItem("CONDITION_CHANGED")))
        .andExpect(jsonPath("$[*].reason").value(hasItem("Lente con fisura visible")));
  }

  @Test
  void returnLoanWithIncidentWithoutDescriptionReturnsBadRequest() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Aula de Medios");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-013", "Tablet Samsung");
    UUID loanId = createLoan(accessToken, teacher.id(), locationId, assetId);

    mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "assetReviews": [
                    {
                      "assetId": "%s",
                      "hasIncident": true,
                      "incidentDescription": "   ",
                      "conditionAfterReturn": "Malo"
                    }
                  ]
                }
                """.formatted(assetId)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnLoanWithAssetOutsideLoanReturnsBadRequest() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Depósito Técnico");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-014", "Monitor LG");
    UUID outsideAssetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-015", "Monitor Samsung");
    UUID loanId = createLoan(accessToken, teacher.id(), locationId, assetId);

    mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "assetReviews": [
                    {
                      "assetId": "%s",
                      "hasIncident": true,
                      "incidentDescription": "No corresponde al préstamo",
                      "conditionAfterReturn": "Regular"
                    }
                  ]
                }
                """.formatted(outsideAssetId)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnAlreadyReturnedLoanReturnsConflict() throws Exception {
    String accessToken = createAdminAndLogin();
    TeacherFixture teacher = createTeacher(accessToken);
    UUID locationId = createLocation(accessToken, "Biblioteca");
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID assetId = createAsset(accessToken, catalog.assetTypeId(), locationId, "CMP-2026-016", "Laptop Acer");
    UUID loanId = createLoan(accessToken, teacher.id(), locationId, assetId);

    mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isConflict());
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
  }

  private TeacherFixture createTeacher(String accessToken) throws Exception {
    String response = mockMvc.perform(post("/api/teachers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "dni": "45678912",
                  "fullName": "Alejandro Cárdenas",
                  "specialty": "Matemáticas y Física",
                  "email": "a.cardenas@colegio.edu.pe",
                  "phone": "+51 987 654 321",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return new TeacherFixture(
        UUID.fromString(objectMapper.readTree(response).get("id").asText()),
        objectMapper.readTree(response).get("fullName").asText()
    );
  }

  private UUID createLocation(String accessToken, String name) throws Exception {
    String response = mockMvc.perform(post("/api/locations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "description": "Ubicación destino de préstamos.",
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

  private UUID createAsset(
      String accessToken,
      UUID assetTypeId,
      UUID locationId,
      String code,
      String name
  ) throws Exception {
    String response = mockMvc.perform(multipart("/api/assets")
            .file(new MockMultipartFile(
                "payload",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                    {
                      "code": "%s",
                      "name": "%s",
                      "assetTypeId": "%s",
                      "locationId": "%s",
                      "condition": "Bueno",
                      "serialNumber": "%s-SN",
                      "acquisitionDate": "2026-01-15",
                      "notes": "Activo para préstamo",
                      "attributeValues": [],
                      "removedAttachmentIds": []
                    }
                    """.formatted(code, name, assetTypeId, locationId, code).getBytes(StandardCharsets.UTF_8)
            ))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private UUID createLoan(
      String accessToken,
      UUID teacherId,
      UUID locationId,
      UUID assetId
  ) throws Exception {
    MockMultipartFile payload = new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        """
            {
              "teacherId": "%s",
              "destinationLocationId": "%s",
              "loanDate": "2026-05-14",
              "dueDate": "2026-05-20",
              "notes": "Préstamo para devolución",
              "assetIds": ["%s"],
              "attachmentSources": []
            }
            """.formatted(teacherId, locationId, assetId).getBytes(StandardCharsets.UTF_8)
    );

    String response = mockMvc.perform(multipart("/api/loans")
            .file(payload)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private record AssetCatalog(UUID categoryId, UUID assetTypeId) {}

  private record TeacherFixture(UUID id, String fullName) {}
}
