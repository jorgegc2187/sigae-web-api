package com.sigae.api.model.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationItemResponse(
    UUID id,
    String type,
    String severity,
    String title,
    String message,
    String route,
    Instant occurredAt,
    boolean read,
    boolean active
) {}
