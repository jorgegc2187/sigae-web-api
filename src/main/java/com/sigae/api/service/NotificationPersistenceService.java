package com.sigae.api.service;

import com.sigae.api.model.entity.AppNotification;
import com.sigae.api.model.entity.NotificationSeverity;
import com.sigae.api.model.entity.NotificationType;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.model.entity.UserMfaSettings;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.AppNotificationRepository;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.repository.PasswordResetRequestRepository;
import com.sigae.api.repository.UserMfaSettingsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPersistenceService {

  private static final UUID GLOBAL_SCOPE_PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final AppNotificationRepository appNotificationRepository;
  private final LoanRepository loanRepository;
  private final PasswordResetRequestRepository passwordResetRequestRepository;
  private final UserMfaSettingsRepository userMfaSettingsRepository;

  public NotificationPersistenceService(
      AppNotificationRepository appNotificationRepository,
      LoanRepository loanRepository,
      PasswordResetRequestRepository passwordResetRequestRepository,
      UserMfaSettingsRepository userMfaSettingsRepository
  ) {
    this.appNotificationRepository = appNotificationRepository;
    this.loanRepository = loanRepository;
    this.passwordResetRequestRepository = passwordResetRequestRepository;
    this.userMfaSettingsRepository = userMfaSettingsRepository;
  }

  @Transactional
  public void synchronizeActiveNotifications() {
    LocalDate today = LocalDate.now();
    Instant now = Instant.now();

    List<NotificationCandidate> candidates = new ArrayList<>(buildLoanCandidates(today));
    candidates.addAll(buildInvitationCandidates(now));
    candidates.addAll(buildMfaCandidates());

    Map<String, AppNotification> existingByKey = appNotificationRepository.findAllByExternalKeyIn(
            candidates.stream().map(NotificationCandidate::externalKey).toList()
        )
        .stream()
        .collect(Collectors.toMap(AppNotification::getExternalKey, Function.identity()));

    for (NotificationCandidate candidate : candidates) {
      AppNotification existing = existingByKey.get(candidate.externalKey());
      if (existing == null) {
        appNotificationRepository.save(candidate.toEntity());
        continue;
      }

      existing.setType(candidate.type());
      existing.setSeverity(candidate.severity());
      existing.setTitle(candidate.title());
      existing.setMessage(candidate.message());
      existing.setRoute(candidate.route());
      existing.setOccurredAt(candidate.occurredAt());
      existing.setActive(true);
      existing.setAdminOnly(candidate.adminOnly());
      existing.setRelatedLocationId(candidate.relatedLocationId());
    }

    List<String> candidateKeys = candidates.stream().map(NotificationCandidate::externalKey).toList();
    Collection<NotificationType> managedTypes = EnumSet.allOf(NotificationType.class);
    if (candidateKeys.isEmpty()) {
      appNotificationRepository.findVisibleForUser(true, true, false, List.of(GLOBAL_SCOPE_PLACEHOLDER))
          .stream()
          .filter(AppNotification::isActive)
          .filter(notification -> managedTypes.contains(notification.getType()))
          .forEach(notification -> notification.setActive(false));
      return;
    }

    appNotificationRepository.findActiveManagedNotificationsToDeactivate(managedTypes, candidateKeys)
        .forEach(notification -> notification.setActive(false));
  }

  private List<NotificationCandidate> buildLoanCandidates(LocalDate today) {
    return loanRepository.findLiveAttentionNotifications(today, false, List.of(GLOBAL_SCOPE_PLACEHOLDER))
        .stream()
        .map(loan -> {
          boolean overdue = loan.getDueDate().isBefore(today);
          return new NotificationCandidate(
              overdue ? "loan-overdue-" + loan.getId() : "loan-due-today-" + loan.getId(),
              overdue ? NotificationType.LOAN_OVERDUE : NotificationType.LOAN_DUE_TODAY,
              overdue ? NotificationSeverity.ERROR : NotificationSeverity.WARNING,
              overdue ? "Préstamo vencido" : "Préstamo vence hoy",
              overdue
                  ? "%s de %s venció el %s.".formatted(loan.getCode(), loan.getTeacherNameSnapshot(), loan.getDueDate())
                  : "%s de %s debe devolverse hoy.".formatted(loan.getCode(), loan.getTeacherNameSnapshot()),
              "/loans/" + loan.getId(),
              loan.getDueDate().atStartOfDay().toInstant(ZoneOffset.UTC),
              false,
              loan.getDestinationLocation() == null ? null : loan.getDestinationLocation().getId()
          );
        })
        .toList();
  }

  private List<NotificationCandidate> buildInvitationCandidates(Instant now) {
    return passwordResetRequestRepository.findActiveForLiveNotifications(PasswordResetPurpose.ACCOUNT_SETUP, UserStatus.PENDING, now)
        .stream()
        .map(this::toInvitationCandidate)
        .toList();
  }

  private List<NotificationCandidate> buildMfaCandidates() {
    return userMfaSettingsRepository.findPendingForLiveNotifications(UserStatus.ACTIVE)
        .stream()
        .map(this::toMfaCandidate)
        .toList();
  }

  private NotificationCandidate toInvitationCandidate(PasswordResetRequest request) {
    return new NotificationCandidate(
        "invitation-pending-" + request.getUser().getId(),
        NotificationType.USER_INVITATION_PENDING,
        NotificationSeverity.INFO,
        "Invitación pendiente",
        "%s aún no completa la activación de su cuenta.".formatted(request.getUser().getFullName()),
        "/settings/users/" + request.getUser().getId() + "/edit",
        request.getCreatedAt(),
        true,
        null
    );
  }

  private NotificationCandidate toMfaCandidate(UserMfaSettings settings) {
    return new NotificationCandidate(
        "mfa-pending-" + settings.getUser().getId(),
        NotificationType.USER_MFA_PENDING,
        NotificationSeverity.WARNING,
        "2FA pendiente de enrolamiento",
        "%s tiene 2FA requerido, pero aún no completa el enrolamiento.".formatted(settings.getUser().getFullName()),
        "/settings/users/" + settings.getUser().getId() + "/edit",
        settings.getUpdatedAt(),
        true,
        null
    );
  }

  private record NotificationCandidate(
      String externalKey,
      NotificationType type,
      NotificationSeverity severity,
      String title,
      String message,
      String route,
      Instant occurredAt,
      boolean adminOnly,
      UUID relatedLocationId
  ) {
    private AppNotification toEntity() {
      return new AppNotification(
          externalKey,
          type,
          severity,
          title,
          message,
          route,
          occurredAt,
          adminOnly,
          relatedLocationId
      );
    }
  }
}
