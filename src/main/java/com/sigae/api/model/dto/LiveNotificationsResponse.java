package com.sigae.api.model.dto;

import java.time.Instant;
import java.util.List;

public record LiveNotificationsResponse(
    long totalActiveCount,
    long loanAttentionCount,
    List<LiveNotificationItem> items
) {
  public record LiveNotificationItem(
      String id,
      String type,
      String severity,
      String title,
      String message,
      String route,
      Instant occurredAt
  ) {}
}
