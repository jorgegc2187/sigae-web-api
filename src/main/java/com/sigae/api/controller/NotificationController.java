package com.sigae.api.controller;

import com.sigae.api.model.dto.LiveNotificationsResponse;
import com.sigae.api.model.dto.NotificationsPageResponse;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.service.LiveNotificationService;
import com.sigae.api.service.NotificationService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final LiveNotificationService liveNotificationService;
  private final NotificationService notificationService;

  public NotificationController(
      LiveNotificationService liveNotificationService,
      NotificationService notificationService
  ) {
    this.liveNotificationService = liveNotificationService;
    this.notificationService = notificationService;
  }

  @GetMapping("/live")
  public LiveNotificationsResponse live(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return liveNotificationService.snapshot(authenticatedUser);
  }

  @GetMapping
  public NotificationsPageResponse list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(defaultValue = "all") String filter,
      @RequestParam(defaultValue = "8") int limit,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "false") boolean includeResolved
  ) {
    return notificationService.list(authenticatedUser, filter, includeResolved, limit, offset);
  }

  @PostMapping("/{notificationId}/read")
  public void markAsRead(
      @PathVariable UUID notificationId,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    notificationService.markAsRead(notificationId, authenticatedUser);
  }

  @PostMapping("/read-all")
  public void markAllAsRead(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam(defaultValue = "all") String filter,
      @RequestParam(defaultValue = "false") boolean includeResolved
  ) {
    notificationService.markAllAsRead(authenticatedUser, filter, includeResolved);
  }
}
