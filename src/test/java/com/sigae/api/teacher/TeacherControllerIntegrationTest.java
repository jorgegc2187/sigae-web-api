package com.sigae.api.teacher;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Teacher;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.TeacherRepository;
import com.sigae.api.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherControllerIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private TeacherRepository teacherRepository;

  @Test
  void adminCanCreateAndUpdateTeacher() throws Exception {
    String accessToken = createAdminAndLogin();

    String response = mockMvc.perform(post("/api/teachers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "dni": "12345678",
                  "fullName": "Mariela Soto",
                  "specialty": "Matemática",
                  "email": "mariela.soto@sigae.edu.pe",
                  "phone": "999888777",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fullName").value("Mariela Soto"))
        .andExpect(jsonPath("$.status").value("Activo"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String teacherId = objectMapper.readTree(response).get("id").asText();

    mockMvc.perform(patch("/api/teachers/{teacherId}", teacherId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "dni": "12345678",
                  "fullName": "Mariela Soto Rivera",
                  "specialty": "Física",
                  "email": "mariela.rivera@sigae.edu.pe",
                  "phone": "999888111",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Mariela Soto Rivera"))
        .andExpect(jsonPath("$.specialty").value("Física"));
  }

  @Test
  void rejectsDuplicateDni() throws Exception {
    String accessToken = createAdminAndLogin();
    teacherRepository.save(new Teacher(
        "87654321",
        "Carlos Vega",
        "Historia",
        "carlos.vega@sigae.edu.pe",
        "955444333",
        CatalogStatus.ACTIVE
    ));

    mockMvc.perform(post("/api/teachers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "dni": "87654321",
                  "fullName": "Nuevo Docente",
                  "specialty": "Comunicación",
                  "email": "nuevo.docente@sigae.edu.pe",
                  "phone": "944111222",
                  "status": "Activo"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Ya existe un docente con ese DNI."));
  }

  @Test
  void adminCanDeactivateAndFilterTeachers() throws Exception {
    String accessToken = createAdminAndLogin();
    Teacher activeTeacher = teacherRepository.save(new Teacher(
        "11112222",
        "Docente Activo",
        "Arte",
        "activo@sigae.edu.pe",
        "911111111",
        CatalogStatus.ACTIVE
    ));
    teacherRepository.save(new Teacher(
        "33334444",
        "Docente Inactivo",
        "Música",
        "inactivo@sigae.edu.pe",
        "922222222",
        CatalogStatus.INACTIVE
    ));

    mockMvc.perform(patch("/api/teachers/{teacherId}/status", activeTeacher.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "Inactivo"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Inactivo"));

    mockMvc.perform(get("/api/teachers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc.perform(get("/api/teachers")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .param("status", "INACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  private String createAdminAndLogin() throws Exception {
    createUser("Admin Teachers", "admin.teachers@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    return loginAndGetAccessToken("admin.teachers@sigae.edu.pe", "admin123456");
  }
}
