package com.sigae.api.service;

import com.sigae.api.model.dto.LiveNotificationsResponse;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.model.entity.UserMfaSettings;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.PasswordResetRequestRepository;
import com.sigae.api.repository.UserMfaSettingsRepository;
import com.sigae.api.security.AuthenticatedUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LiveNotificationService {

  private static final UUID GLOBAL_SCOPE_PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final LoanRepository loanRepository;
  private final PasswordResetRequestRepository passwordResetRequestRepository;
  private final UserMfaSettingsRepository userMfaSettingsRepository;

  public LiveNotificationService(
      LoanRepository loanRepository,
      PasswordResetRequestRepository passwordResetRequestRepository,
      UserMfaSettingsRepository userMfaSettingsRepository
  ) {
    this.loanRepository = loanRepository;
    this.passwordResetRequestRepository = passwordResetRequestRepository;
    this.userMfaSettingsRepository = userMfaSettingsRepository;
  }

  public LiveNotificationsResponse snapshot(AuthenticatedUser authenticatedUser) {
    NotificationScope scope = resolveScope(authenticatedUser);
    LocalDate today = LocalDate.now();

    List<LiveNotificationsResponse.LiveNotificationItem> loanItems = loanRepository
        .findLiveAttentionNotifications(today, scope.applyScope(), scope.locationIds())
        .stream()
        .map(loan -> toLoanItem(loan, today))
        .toList();

    List<LiveNotificationsResponse.LiveNotificationItem> invitationItems = isAdministrator(authenticatedUser)
        ? passwordResetRequestRepository.findActiveForLiveNotifications(PasswordResetPurpose.ACCOUNT_SETUP, UserStatus.PENDING, Instant.now())
            .stream()
            .map(this::toInvitationItem)
            .toList()
        : List.of();

    List<LiveNotificationsResponse.LiveNotificationItem> mfaItems = isAdministrator(authenticatedUser)
        ? userMfaSettingsRepository.findPendingForLiveNotifications(UserStatus.ACTIVE)
            .stream()
            .map(this::toMfaItem)
            .toList()
        : List.of();

    List<LiveNotificationsResponse.LiveNotificationItem> items = Stream.of(loanItems, invitationItems, mfaItems)
        .flatMap(List::stream)
        .sorted(notificationComparator())
        .toList();

    return new LiveNotificationsResponse(items.size(), loanItems.size(), items);
  }

  private LiveNotificationsResponse.LiveNotificationItem toLoanItem(Loan loan, LocalDate today) {
    boolean overdue = loan.getDueDate().isBefore(today);
    String idPrefix = overdue ? "loan-overdue-" : "loan-due-today-";
    String title = overdue ? "Préstamo vencido" : "Préstamo vence hoy";
    String message = overdue
        ? "%s de %s venció el %s.".formatted(loan.getCode(), loan.getTeacherNameSnapshot(), loan.getDueDate())
        : "%s de %s debe devolverse hoy.".formatted(loan.getCode(), loan.getTeacherNameSnapshot());

    return new LiveNotificationsResponse.LiveNotificationItem(
        idPrefix + loan.getId(),
        overdue ? "loan_overdue" : "loan_due_today",
        overdue ? "error" : "warning",
        title,
        message,
        "/loans/" + loan.getId(),
        loan.getDueDate().atStartOfDay().toInstant(ZoneOffset.UTC)
    );
  }

  private LiveNotificationsResponse.LiveNotificationItem toInvitationItem(PasswordResetRequest request) {
    return new LiveNotificationsResponse.LiveNotificationItem(
        "invitation-pending-" + request.getUser().getId(),
        "user_invitation_pending",
        "info",
        "Invitación pendiente",
        "%s aún no completa la activación de su cuenta.".formatted(request.getUser().getFullName()),
        "/settings/users/" + request.getUser().getId() + "/edit",
        request.getCreatedAt()
    );
  }

  private LiveNotificationsResponse.LiveNotificationItem toMfaItem(UserMfaSettings settings) {
    return new LiveNotificationsResponse.LiveNotificationItem(
        "mfa-pending-" + settings.getUser().getId(),
        "user_mfa_pending",
        "warning",
        "2FA pendiente de enrolamiento",
        "%s tiene 2FA requerido, pero aún no completa el enrolamiento.".formatted(settings.getUser().getFullName()),
        "/settings/users/" + settings.getUser().getId() + "/edit",
        settings.getUpdatedAt()
    );
  }

  private Comparator<LiveNotificationsResponse.LiveNotificationItem> notificationComparator() {
    return Comparator
        .comparingInt((LiveNotificationsResponse.LiveNotificationItem item) -> severityOrder(item.severity()))
        .thenComparing(LiveNotificationsResponse.LiveNotificationItem::occurredAt, Comparator.reverseOrder());
  }

  private int severityOrder(String severity) {
    return switch (severity) {
      case "error" -> 0;
      case "warning" -> 1;
      default -> 2;
    };
  }

  private NotificationScope resolveScope(AuthenticatedUser authenticatedUser) {
    if (authenticatedUser == null || authenticatedUser.role() != UserRole.ENCARGADO) {
      return new NotificationScope(false, List.of(GLOBAL_SCOPE_PLACEHOLDER));
    }

    List<UUID> locationIds = authenticatedUser.locationIds().stream()
        .map(UUID::fromString)
        .toList();

    return locationIds.isEmpty()
        ? new NotificationScope(false, List.of(GLOBAL_SCOPE_PLACEHOLDER))
        : new NotificationScope(true, locationIds);
  }

  private boolean isAdministrator(AuthenticatedUser authenticatedUser) {
    return authenticatedUser != null && authenticatedUser.role() == UserRole.ADMINISTRADOR;
  }

  private record NotificationScope(boolean applyScope, List<UUID> locationIds) {}
}
