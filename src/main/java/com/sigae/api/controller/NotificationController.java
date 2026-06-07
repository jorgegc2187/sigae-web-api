package com.sigae.api.controller;

import com.sigae.api.model.dto.LiveNotificationsResponse;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.service.LiveNotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final LiveNotificationService liveNotificationService;

  public NotificationController(LiveNotificationService liveNotificationService) {
    this.liveNotificationService = liveNotificationService;
  }

  @GetMapping("/live")
  public LiveNotificationsResponse live(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return liveNotificationService.snapshot(authenticatedUser);
  }
}
