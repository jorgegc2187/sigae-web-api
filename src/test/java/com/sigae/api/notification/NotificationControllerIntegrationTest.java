package com.sigae.api.notification;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.model.entity.Teacher;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserMfaSettings;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.TeacherRepository;
import com.sigae.api.repository.UserMfaSettingsRepository;
import com.sigae.api.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private TeacherRepository teacherRepository;

  @Autowired
  private LoanRepository loanRepository;

  @Autowired
  private UserMfaSettingsRepository userMfaSettingsRepository;

  @Test
  void adminReceivesLoanAndSecurityLiveNotifications() throws Exception {
    User admin = createUser("Admin Principal", "admin-notif@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    User pendingInvite = createUser("Ana Pendiente", "ana-pendiente@sigae.edu.pe", "Password123!", UserRole.ENCARGADO, UserStatus.PENDING);
    User activeMfaUser = createUser("Marco Seguro", "marco-seguro@sigae.edu.pe", "Password123!", UserRole.ENCARGADO, UserStatus.ACTIVE);
    String accessToken = loginAndGetAccessToken(admin.getEmail(), "admin123456");

    passwordResetRequestRepository.save(new PasswordResetRequest(
        pendingInvite,
        "invitation-token-hash",
        Instant.now().plusSeconds(3_600),
        PasswordResetPurpose.ACCOUNT_SETUP
    ));

    UserMfaSettings settings = new UserMfaSettings(activeMfaUser);
    settings.setMfaRequired(true);
    userMfaSettingsRepository.save(settings);

    Location lab = createLocation("Laboratorio Vivo");
    createLoan("PRE-2026-1001", lab, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
    createLoan("PRE-2026-1002", lab, LocalDate.now(), LocalDate.now());

    mockMvc.perform(get("/api/notifications/live")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loanAttentionCount").value(2))
        .andExpect(jsonPath("$.totalActiveCount").value(4))
        .andExpect(jsonPath("$.items", hasSize(4)))
        .andExpect(jsonPath("$.items[0].type").value("loan_overdue"))
        .andExpect(jsonPath("$.items[*].type", hasItems(
            "loan_due_today",
            "user_mfa_pending",
            "user_invitation_pending"
        )));
  }

  @Test
  void encargadoReceivesOnlyScopedLoanNotifications() throws Exception {
    createUser("Admin Principal", "admin-notif@sigae.edu.pe", "admin123456", UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    User encargado = createUser("Erika Encargada", "encargada-notif@sigae.edu.pe", "encargado123", UserRole.ENCARGADO, UserStatus.ACTIVE);

    Location lab = createLocation("Laboratorio Encargado");
    Location library = createLocation("Biblioteca General");
    encargado.setLocations(Set.of(lab));
    userRepository.save(encargado);
    String accessToken = loginAndGetAccessToken("encargada-notif@sigae.edu.pe", "encargado123");

    createLoan("PRE-2026-2001", lab, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1));
    createLoan("PRE-2026-2002", library, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1));

    mockMvc.perform(get("/api/notifications/live")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loanAttentionCount").value(1))
        .andExpect(jsonPath("$.totalActiveCount").value(1))
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("loan_overdue"))
        .andExpect(jsonPath("$.items[0].route").exists());
  }

  private void createLoan(String code, Location destination, LocalDate loanDate, LocalDate dueDate) {
    Teacher teacher = teacherRepository.save(new Teacher(
        "DOC%s".formatted(code.substring(code.length() - 4)),
        "Docente %s".formatted(code.substring(code.length() - 4)),
        "Tecnología",
        "%s@sigae.edu.pe".formatted(code.toLowerCase()),
        "999999999",
        CatalogStatus.ACTIVE
    ));

    loanRepository.save(new Loan(code, teacher, destination, loanDate, dueDate, "Préstamo notificaciones"));
  }
}
