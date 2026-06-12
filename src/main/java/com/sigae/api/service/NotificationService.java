package com.sigae.api.service;

import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.NotificationItemResponse;
import com.sigae.api.model.dto.NotificationsPageResponse;
import com.sigae.api.model.entity.AppNotification;
import com.sigae.api.model.entity.NotificationType;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserNotificationState;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.repository.AppNotificationRepository;
import com.sigae.api.repository.UserNotificationStateRepository;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final UUID GLOBAL_SCOPE_PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final NotificationPersistenceService notificationPersistenceService;
  private final AppNotificationRepository appNotificationRepository;
  private final UserNotificationStateRepository userNotificationStateRepository;
  private final UserRepository userRepository;

  public NotificationService(
      NotificationPersistenceService notificationPersistenceService,
      AppNotificationRepository appNotificationRepository,
      UserNotificationStateRepository userNotificationStateRepository,
      UserRepository userRepository
  ) {
    this.notificationPersistenceService = notificationPersistenceService;
    this.appNotificationRepository = appNotificationRepository;
    this.userNotificationStateRepository = userNotificationStateRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public NotificationsPageResponse list(
      AuthenticatedUser authenticatedUser,
      String filter,
      boolean includeResolved,
      int limit,
      int offset
  ) {
    notificationPersistenceService.synchronizeActiveNotifications();
    ResolvedUserScope scope = resolveUserScope(authenticatedUser);
    List<AppNotification> visible = appNotificationRepository.findVisibleForUser(
        includeResolved,
        scope.includeAdminOnly(),
        scope.applyLocationScope(),
        scope.locationIds()
    );

    Map<UUID, UserNotificationState> readStatesByNotificationId = visible.isEmpty()
        ? Map.of()
        : userNotificationStateRepository.findByUser_IdAndNotification_IdIn(
                scope.user().getId(),
                visible.stream().map(AppNotification::getId).toList()
            )
            .stream()
            .collect(Collectors.toMap(state -> state.getNotification().getId(), Function.identity()));

    List<AppNotification> filtered = visible.stream()
        .filter(notification -> !"unread".equalsIgnoreCase(filter) || !isRead(notification, readStatesByNotificationId))
        .toList();

    int safeOffset = Math.max(offset, 0);
    int safeLimit = Math.max(limit, 1);
    int fromIndex = Math.min(safeOffset, filtered.size());
    int toIndex = Math.min(fromIndex + safeLimit, filtered.size());

    List<NotificationItemResponse> items = filtered.subList(fromIndex, toIndex).stream()
        .map(notification -> toResponse(notification, isRead(notification, readStatesByNotificationId)))
        .toList();

    long unreadCount = visible.stream()
        .filter(AppNotification::isActive)
        .filter(notification -> !isRead(notification, readStatesByNotificationId))
        .count();

    long loanAttentionCount = visible.stream()
        .filter(AppNotification::isActive)
        .filter(notification -> notification.getType() == NotificationType.LOAN_OVERDUE || notification.getType() == NotificationType.LOAN_DUE_TODAY)
        .count();

    return new NotificationsPageResponse(filtered.size(), unreadCount, loanAttentionCount, items);
  }

  @Transactional
  public void markAsRead(UUID notificationId, AuthenticatedUser authenticatedUser) {
    notificationPersistenceService.synchronizeActiveNotifications();
    ResolvedUserScope scope = resolveUserScope(authenticatedUser);
    AppNotification notification = loadVisibleNotification(notificationId, scope);
    UserNotificationState state = userNotificationStateRepository.findByNotification_IdAndUser_Id(notification.getId(), scope.user().getId())
        .orElseGet(() -> new UserNotificationState(notification, scope.user()));
    state.markAsRead(Instant.now());
    userNotificationStateRepository.save(state);
  }

  @Transactional
  public void markAllAsRead(
      AuthenticatedUser authenticatedUser,
      String filter,
      boolean includeResolved
  ) {
    notificationPersistenceService.synchronizeActiveNotifications();
    ResolvedUserScope scope = resolveUserScope(authenticatedUser);
    List<AppNotification> visible = appNotificationRepository.findVisibleForUser(
        includeResolved,
        scope.includeAdminOnly(),
        scope.applyLocationScope(),
        scope.locationIds()
    );

    if (visible.isEmpty()) {
      return;
    }

    Map<UUID, UserNotificationState> existingStates = userNotificationStateRepository.findByUser_IdAndNotification_IdIn(
            scope.user().getId(),
            visible.stream().map(AppNotification::getId).toList()
        )
        .stream()
        .collect(Collectors.toMap(state -> state.getNotification().getId(), Function.identity()));

    Instant now = Instant.now();
    for (AppNotification notification : visible) {
      if ("unread".equalsIgnoreCase(filter) && isRead(notification, existingStates)) {
        continue;
      }

      UserNotificationState state = existingStates.get(notification.getId());
      if (state == null) {
        state = new UserNotificationState(notification, scope.user());
        existingStates.put(notification.getId(), state);
      }
      state.markAsRead(now);
      userNotificationStateRepository.save(state);
    }
  }

  private AppNotification loadVisibleNotification(UUID notificationId, ResolvedUserScope scope) {
    return appNotificationRepository.findVisibleForUser(true, scope.includeAdminOnly(), scope.applyLocationScope(), scope.locationIds())
        .stream()
        .filter(notification -> notification.getId().equals(notificationId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Notificación no encontrada."));
  }

  private boolean isRead(AppNotification notification, Map<UUID, UserNotificationState> readStatesByNotificationId) {
    UserNotificationState state = readStatesByNotificationId.get(notification.getId());
    return state != null && state.getReadAt() != null;
  }

  private NotificationItemResponse toResponse(AppNotification notification, boolean read) {
    return new NotificationItemResponse(
        notification.getId(),
        notification.getType().name().toLowerCase(),
        notification.getSeverity().name().toLowerCase(),
        notification.getTitle(),
        notification.getMessage(),
        notification.getRoute(),
        notification.getOccurredAt(),
        read,
        notification.isActive()
    );
  }

  private ResolvedUserScope resolveUserScope(AuthenticatedUser authenticatedUser) {
    if (authenticatedUser == null) {
      throw new NotFoundException("Usuario autenticado no encontrado.");
    }

    User user = userRepository.findById(authenticatedUser.userId())
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));

    boolean includeAdminOnly = authenticatedUser.role() == UserRole.ADMINISTRADOR;
    if (authenticatedUser.role() != UserRole.ENCARGADO) {
      return new ResolvedUserScope(user, includeAdminOnly, false, List.of(GLOBAL_SCOPE_PLACEHOLDER));
    }

    List<UUID> locationIds = authenticatedUser.locationIds().stream().map(UUID::fromString).toList();
    if (locationIds.isEmpty()) {
      return new ResolvedUserScope(user, false, false, List.of(GLOBAL_SCOPE_PLACEHOLDER));
    }

    return new ResolvedUserScope(user, false, true, locationIds);
  }

  private record ResolvedUserScope(
      User user,
      boolean includeAdminOnly,
      boolean applyLocationScope,
      Collection<UUID> locationIds
  ) {}
}
