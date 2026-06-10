package com.sigae.api.dashboard;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.LoanAsset;
import com.sigae.api.model.entity.Teacher;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.TeacherRepository;
import com.sigae.api.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private TeacherRepository teacherRepository;

  @Autowired
  private LoanRepository loanRepository;

  @Autowired
  private AssetRepository assetRepository;

  @Test
  void adminReceivesGlobalDashboardOverview() throws Exception {
    String adminToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(adminToken);
    Location lab = createLocation("Laboratorio A");
    Location library = createLocation("Biblioteca");

    UUID buenoAssetId = createAsset(adminToken, catalog.assetTypeId(), lab.getId(), "CMP-2026-001", "Laptop Lenovo", "Bueno");
    createAsset(adminToken, catalog.assetTypeId(), lab.getId(), "CMP-2026-002", "Laptop HP", "Regular");
    UUID maintenanceAssetId = createAsset(adminToken, catalog.assetTypeId(), lab.getId(), "CMP-2026-003", "Proyector Epson", "Mantenimiento");
    UUID lowAssetId = createAsset(adminToken, catalog.assetTypeId(), library.getId(), "CMP-2026-004", "Tablet Samsung", "Bueno");

    updateAssetCondition(adminToken, lowAssetId, catalog.assetTypeId(), library.getId(), "Dado de baja", "Tablet Samsung");
    createLoan(lab, buenoAssetId, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), "PRE-2026-0001");
    createLoan(lab, maintenanceAssetId, LocalDate.now(), LocalDate.now(), "PRE-2026-0002");

    mockMvc.perform(get("/api/dashboard/overview")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metrics.totalAssets").value(4))
        .andExpect(jsonPath("$.metrics.operationalAssets").value(2))
        .andExpect(jsonPath("$.metrics.maintenanceAssets").value(1))
        .andExpect(jsonPath("$.metrics.decommissionedAssets").value(1))
        .andExpect(jsonPath("$.metrics.activeLoans").value(2))
        .andExpect(jsonPath("$.metrics.overdueLoans").value(1))
        .andExpect(jsonPath("$.metrics.dueTodayLoans").value(1))
        .andExpect(jsonPath("$.conditionBreakdown.good").value(1))
        .andExpect(jsonPath("$.conditionBreakdown.regular").value(1))
        .andExpect(jsonPath("$.conditionBreakdown.maintenance").value(1))
        .andExpect(jsonPath("$.conditionBreakdown.decommissioned").value(1))
        .andExpect(jsonPath("$.loanAlerts", hasSize(2)))
        .andExpect(jsonPath("$.loanAlerts[0].severity").value("overdue"))
        .andExpect(jsonPath("$.loanAlerts[1].severity").value("due_today"))
        .andExpect(jsonPath("$.recentMovements[0].assetCode").value("CMP-2026-004"));
  }

  @Test
  void encargadoOnlyReceivesDashboardDataForAssignedLocations() throws Exception {
    String adminToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(adminToken);
    Location lab = createLocation("Laboratorio Encargado");
    Location library = createLocation("Biblioteca General");

    UUID scopedAssetId = createAsset(adminToken, catalog.assetTypeId(), lab.getId(), "CMP-2026-010", "Laptop Dell", "Bueno");
    createAsset(adminToken, catalog.assetTypeId(), library.getId(), "CMP-2026-011", "Tablet Lenovo", "Mantenimiento");
    createLoan(lab, scopedAssetId, LocalDate.now(), LocalDate.now().plusDays(2), "PRE-2026-0010");

    User encargado = createUser("Erika Flores", "encargado@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);
    encargado.setLocations(Set.of(lab));
    userRepository.save(encargado);
    String encargadoToken = loginAndGetAccessToken("encargado@sigae.edu.pe", "encargado123");

    mockMvc.perform(get("/api/dashboard/overview")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + encargadoToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metrics.totalAssets").value(1))
        .andExpect(jsonPath("$.metrics.activeLoans").value(1))
        .andExpect(jsonPath("$.topCategories", hasSize(1)))
        .andExpect(jsonPath("$.loanAlerts", hasSize(1)))
        .andExpect(jsonPath("$.recentMovements[0].assetCode").value("CMP-2026-010"));
  }

  @Test
  void soloLecturaCanReadGlobalDashboardOverview() throws Exception {
    String adminToken = createAdminAndLogin();
    AssetCatalog catalog = createAssetCatalog(adminToken);
    Location lab = createLocation("Laboratorio de Lectura");
    createAsset(adminToken, catalog.assetTypeId(), lab.getId(), "CMP-2026-020", "Equipo de lectura", "Bueno");

    createUser("Ana Lectura", "lectura-dashboard@sigae.edu.pe", "lectura123", UserRole.SOLO_LECTURA, UserStatus.ACTIVE);
    String readOnlyToken = loginAndGetAccessToken("lectura-dashboard@sigae.edu.pe", "lectura123");

    mockMvc.perform(get("/api/dashboard/overview")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + readOnlyToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metrics.totalAssets").value(1));
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Carlos Mendoza", "admin-dashboard@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin-dashboard@sigae.edu.pe", "admin123456");
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
                      "description": "Fabricante del activo",
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
      String name,
      String condition
  ) throws Exception {
    String response = mockMvc.perform(multipart("/api/assets")
            .file(assetPayload("""
                {
                  "code": "%s",
                  "name": "%s",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "%s",
                  "serialNumber": "%s-SN",
                  "barcode": "BC-%s",
                  "acquisitionDate": "2026-05-20",
                  "notes": "Registro dashboard",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(code, name, assetTypeId, locationId, condition, code, code)))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private void updateAssetCondition(
      String accessToken,
      UUID assetId,
      UUID assetTypeId,
      UUID locationId,
      String condition,
      String name
  ) throws Exception {
    mockMvc.perform(multipart("/api/assets/{assetId}", assetId)
            .file(assetPayload("""
                {
                  "code": "CMP-2026-004",
                  "name": "%s",
                  "assetTypeId": "%s",
                  "locationId": "%s",
                  "condition": "%s",
                  "serialNumber": "CMP-2026-004-SN",
                  "barcode": "BC-CMP-2026-004",
                  "acquisitionDate": "2026-05-20",
                  "notes": "Registro dashboard",
                  "attributeValues": [],
                  "removedAttachmentIds": []
                }
                """.formatted(name, assetTypeId, locationId, condition)))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());
  }

  private MockMultipartFile assetPayload(String content) {
    return new MockMultipartFile(
        "payload",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        content.getBytes(StandardCharsets.UTF_8)
    );
  }

  private void createLoan(
      Location destination,
      UUID assetId,
      LocalDate loanDate,
      LocalDate dueDate,
      String code
  ) {
    Teacher teacher = teacherRepository.save(new Teacher(
        "1234%s".formatted(code.substring(code.length() - 4)),
        "Docente %s".formatted(code.substring(code.length() - 4)),
        "Tecnología",
        "%s@sigae.edu.pe".formatted(code.toLowerCase()),
        "999999999",
        CatalogStatus.ACTIVE
    ));

    Loan loan = new Loan(code, teacher, destination, loanDate, dueDate, "Préstamo dashboard");
    loan.addAsset(new LoanAsset(assetRepository.findById(assetId).orElseThrow()));
    loanRepository.save(loan);
  }

  private record AssetCatalog(UUID categoryId, UUID assetTypeId) {}
}
