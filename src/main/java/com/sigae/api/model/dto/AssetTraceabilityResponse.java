package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.TraceabilityEventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssetTraceabilityResponse(
    UUID id,
    UUID assetId,
    TraceabilityEventType eventType,
    String description,
    String previousValue,
    String newValue,
    String reason,
    UUID userId,
    String userName,
    List<AssetTraceabilityAttachmentResponse> attachments,
    Instant occurredAt
) {
  public static AssetTraceabilityResponse from(AssetTraceability traceability) {
    return new AssetTraceabilityResponse(
        traceability.getId(),
        traceability.getAsset().getId(),
        traceability.getEventType(),
        traceability.getDescription(),
        traceability.getPreviousValue(),
        traceability.getNewValue(),
        traceability.getReason(),
        traceability.getUser() == null ? null : traceability.getUser().getId(),
        traceability.getUser() == null ? "Sistema" : traceability.getUser().getFullName(),
        traceability.getAttachments().stream().map(AssetTraceabilityAttachmentResponse::from).toList(),
        traceability.getOccurredAt()
    );
  }
}
