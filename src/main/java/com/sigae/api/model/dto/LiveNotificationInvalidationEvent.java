package com.sigae.api.model.dto;

import java.time.Instant;

public record LiveNotificationInvalidationEvent(
    String audience,
    Instant occurredAt
) {}
