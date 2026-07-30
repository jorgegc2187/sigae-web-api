package com.sigae.api.reports;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.support.IntegrationTestSupport;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerIntegrationTest extends IntegrationTestSupport {

  private static final byte[] PNG_IMAGE = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLwJAAAAABJRU5ErkJggg=="
  );
  private static final byte[] SIGNATURE_PNG_IMAGE = createPng(Color.BLUE);

  @Test
  void adminCanListAssetReportWithFilters() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createReportLocation(accessToken);
    createAsset(accessToken, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-001", "2026-01-15");

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
  void adminCanListAssetReportWithOnlyDateRange() throws Exception {
    String accessToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(accessToken);
    UUID locationId = createReportLocation(accessToken);
    createAsset(accessToken, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-010", "2026-05-24");

    mockMvc.perform(get("/api/reports/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("startDate", "2026-05-23")
            .param("endDate", "2026-05-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("CMP-2026-010"));
  }

  @Test
  void assetReportRejectsInvalidDateRange() throws Exception {
    String accessToken = createAdminAndLogin();

    mockMvc.perform(get("/api/reports/assets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("startDate", "2026-05-31")
            .param("endDate", "2026-05-23"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la fecha final."));
  }

  @Test
  void soloLecturaCanExportAssetReports() throws Exception {
    String adminToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(adminToken);
    UUID locationId = createReportLocation(adminToken);
    createAsset(adminToken, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-002", "2026-02-15");

    createUser("Ana Lectura", "lectura@sigae.edu.pe", "lectura123", UserRole.SOLO_LECTURA, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken("lectura@sigae.edu.pe", "lectura123");

    assertExport(accessToken, "pdf", "application/pdf", ".pdf");
    assertExport(accessToken, "excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
    assertExport(accessToken, "word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
  }

  @Test
  void physicalInventoryReportIncludesAllAssetsAndHistoricalColumns() throws Exception {
    String adminToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(adminToken);
    UUID locationId = createReportLocation(adminToken);

    createAsset(adminToken, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-100", "2026-02-15");
    UUID decommissionedAssetId = createAsset(adminToken, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-101", "2026-02-16", "Bueno");
    createAsset(adminToken, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-102", "2026-02-17", "Dado de baja");
    updateAssetCondition(adminToken, decommissionedAssetId, catalog.assetTypeId(), catalog.attributeDefinitionId(), locationId, "CMP-2026-101", "Dado de baja");

    mockMvc.perform(get("/api/reports/assets/physical-inventory")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .param("locationId", locationId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("INVENTARIO FÍSICO DE BIENES PATRIMONIALES"))
        .andExpect(jsonPath("$.locationSubtitle").value("LABORATORIO DE CÓMPUTO"))
        .andExpect(jsonPath("$.generatedBy").value("Carlos Mendoza"))
        .andExpect(jsonPath("$.rows.length()").value(3))
        .andExpect(jsonPath("$.rows[0].assetDescription").value("Laptop Lenovo ThinkPad — Modelo T14, serie LNV-T14-001"))
        .andExpect(jsonPath("$.rows[0].brand").value("Lenovo"))
        .andExpect(jsonPath("$.rows[0].good").value(true))
        .andExpect(jsonPath("$.rows[0].quantity").value(1))
        .andExpect(jsonPath("$.rows[1].good").value(false))
        .andExpect(jsonPath("$.rows[1].regular").value(false))
        .andExpect(jsonPath("$.rows[1].bad").value(false))
        .andExpect(jsonPath("$.rows[1].observations").value(containsString("Dado de baja")));

    byte[] wordContent = exportReport(adminToken, "word");
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(wordContent))) {
      String text = extractWordText(document);
      org.junit.jupiter.api.Assertions.assertTrue(text.contains("Carlos Mendoza"));
      org.junit.jupiter.api.Assertions.assertFalse(text.contains("Administrador"));
      org.junit.jupiter.api.Assertions.assertFalse(text.contains("admin@sigae.edu.pe"));
      org.junit.jupiter.api.Assertions.assertTrue(text.contains("INVENTARIO FÍSICO DE BIENES PATRIMONIALES"));
      org.junit.jupiter.api.Assertions.assertTrue(text.contains("ESTADO"));
      org.junit.jupiter.api.Assertions.assertTrue(text.contains("Dado de baja"));
      org.junit.jupiter.api.Assertions.assertFalse(text.contains("CMP-2026-101"));
      org.junit.jupiter.api.Assertions.assertFalse(text.contains("CMP-2026-102"));
    }

    byte[] excelContent = exportReport(adminToken, "excel");
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelContent))) {
      Sheet sheet = workbook.getSheetAt(0);
      List<String> values = extractSheetValues(sheet);
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("INVENTARIO FÍSICO DE BIENES PATRIMONIALES"));
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("ESTADO"));
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("B"));
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("R"));
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("M"));
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("CANTIDAD"));
      org.junit.jupiter.api.Assertions.assertTrue(values.contains("Estado del sistema: Dado de baja | Registro para reporte"));
      org.junit.jupiter.api.Assertions.assertFalse(values.contains("CMP-2026-101"));
      org.junit.jupiter.api.Assertions.assertTrue(sheet.getMergedRegions().stream()
          .anyMatch(range -> range.getFirstColumn() == 3 && range.getLastColumn() == 5));
    }
  }

  @Test
  void reportsRejectAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/reports/assets"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void physicalInventoryExportCanIncludeTemporarySignatureAndInstitutionLogo() throws Exception {
    String accessToken = createAdminAndLogin();
    updateInstitutionSettingsWithLogo(accessToken);

    byte[] wordContent = exportReportWithSignature(accessToken, "word", signaturePngFile());
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(wordContent))) {
      org.junit.jupiter.api.Assertions.assertEquals(2, document.getAllPictures().size());
      org.junit.jupiter.api.Assertions.assertTrue(extractWordText(document).contains("Carlos Mendoza"));
    }

    byte[] excelContent = exportReportWithSignature(accessToken, "excel", signaturePngFile());
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelContent))) {
      org.junit.jupiter.api.Assertions.assertEquals(2, workbook.getAllPictures().size());
      org.junit.jupiter.api.Assertions.assertTrue(extractSheetValues(workbook.getSheetAt(0)).contains("Carlos Mendoza"));
    }

    byte[] pdfContent = exportReportWithSignature(accessToken, "pdf", signaturePngFile());
    org.junit.jupiter.api.Assertions.assertTrue(new String(pdfContent, StandardCharsets.ISO_8859_1).startsWith("%PDF"));

    mockMvc.perform(multipart("/api/reports/assets/export")
            .file(new MockMultipartFile("signature", "firma.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3}))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("format", "pdf"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La firma debe enviarse en formato PNG."));
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

  private byte[] exportReport(String accessToken, String format) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/reports/assets/export")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("format", format))
        .andExpect(status().isOk())
        .andReturn();
    return result.getResponse().getContentAsByteArray();
  }

  private byte[] exportReportWithSignature(
      String accessToken,
      String format,
      MockMultipartFile signature
  ) throws Exception {
    MvcResult result = mockMvc.perform(multipart("/api/reports/assets/export")
            .file(signature)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("format", format))
        .andExpect(status().isOk())
        .andReturn();
    return result.getResponse().getContentAsByteArray();
  }

  private void updateInstitutionSettingsWithLogo(String accessToken) throws Exception {
    MockMultipartFile payload = new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        """
            {
              "systemName": "I.E. Simón Rodríguez - Nasca",
              "address": "Av. Principal 123",
              "city": "Nasca",
              "supportPhone": "",
              "supportEmail": "contacto@sigae.edu.pe"
            }
            """.getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/settings")
            .file(payload)
            .file(pngFile("logo", "logo.png"))
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .with(csrf())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());
  }

  private MockMultipartFile pngFile(String fieldName, String fileName) {
    return new MockMultipartFile(fieldName, fileName, MediaType.IMAGE_PNG_VALUE, PNG_IMAGE);
  }

  private MockMultipartFile signaturePngFile() {
    return new MockMultipartFile("signature", "firma.png", MediaType.IMAGE_PNG_VALUE, SIGNATURE_PNG_IMAGE);
  }

  private static byte[] createPng(Color color) {
    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, color.getRGB());
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo crear la imagen de prueba.", exception);
    }
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Carlos Mendoza", "admin@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin@sigae.edu.pe", "admin123456");
  }

  private UUID createReportLocation(String accessToken) throws Exception {
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

    var assetType = objectMapper.readTree(typeResponse);
    return new AssetCatalog(
        categoryId,
        UUID.fromString(assetType.get("id").asText()),
        UUID.fromString(assetType.get("attributes").get(0).get("id").asText())
    );
  }

  private UUID createAsset(
      String accessToken,
      UUID assetTypeId,
      UUID attributeDefinitionId,
      UUID locationId,
      String code,
      String acquisitionDate
  ) throws Exception {
    return createAsset(accessToken, assetTypeId, attributeDefinitionId, locationId, code, acquisitionDate, "Bueno");
  }

  private UUID createAsset(
      String accessToken,
      UUID assetTypeId,
      UUID attributeDefinitionId,
      UUID locationId,
      String code,
      String acquisitionDate,
      String condition
  ) throws Exception {
    String response = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "%s",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "%s",
                  "serialNumber": "LNV-T14-001",
                  "acquisitionDate": "%s",
                  "description": "Modelo T14, serie LNV-T14-001",
                  "notes": "Registro para reporte",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Lenovo"
                    }
                  ],
                  "removedAttachmentIds": []
                }
                """.formatted(code, assetTypeId, locationId, condition, acquisitionDate, attributeDefinitionId)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private MockMultipartFile assetPayload(String content) {
    return new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        content.getBytes(StandardCharsets.UTF_8)
    );
  }

  private void updateAssetCondition(
      String accessToken,
      UUID assetId,
      UUID assetTypeId,
      UUID attributeDefinitionId,
      UUID locationId,
      String code,
      String condition
  ) throws Exception {
    mockMvc.perform(multipart("/api/assets/%s".formatted(assetId))
            .file(assetPayload("""
                {
                  "code": "%s",
                  "name": "Laptop Lenovo ThinkPad",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "%s",
                  "serialNumber": "LNV-T14-001",
                  "acquisitionDate": "2026-02-16",
                  "description": "Modelo T14, serie LNV-T14-001",
                  "notes": "Registro para reporte",
                  "attributeValues": [
                    {
                      "attributeDefinitionId": "%s",
                      "value": "Lenovo"
                    }
                  ],
                  "removedAttachmentIds": []
                }
                """.formatted(code, assetTypeId, locationId, condition, attributeDefinitionId)))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());
  }

  private String extractWordText(XWPFDocument document) {
    List<String> values = new ArrayList<>();
    document.getParagraphs().forEach(paragraph -> values.add(paragraph.getText()));
    document.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells().forEach(cell -> values.add(cell.getText()))));
    return String.join("\n", values);
  }

  private List<String> extractSheetValues(Sheet sheet) {
    List<String> values = new ArrayList<>();
    sheet.forEach(row -> row.forEach(cell -> values.add(cell.toString())));
    return values;
  }

  private record AssetCatalog(UUID categoryId, UUID assetTypeId, UUID attributeDefinitionId) {}
}
