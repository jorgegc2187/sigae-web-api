package com.sigae.api.model.dto;

import java.util.List;

public record NotificationsPageResponse(
    long totalCount,
    long unreadCount,
    long loanAttentionCount,
    List<NotificationItemResponse> items
) {}
