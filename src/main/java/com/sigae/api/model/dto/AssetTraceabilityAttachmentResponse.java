package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetTraceabilityAttachment;
import java.util.UUID;

public record AssetTraceabilityAttachmentResponse(
    UUID id,
    String fileName,
    String mimeType,
    long sizeBytes,
    String downloadUrl
) {
  public static AssetTraceabilityAttachmentResponse from(AssetTraceabilityAttachment attachment) {
    return new AssetTraceabilityAttachmentResponse(
        attachment.getId(),
        attachment.getFileName(),
        attachment.getMimeType(),
        attachment.getSizeBytes(),
        "/api/assets/%s/traceability/%s/attachments/%s".formatted(
            attachment.getTraceability().getAsset().getId(),
            attachment.getTraceability().getId(),
            attachment.getId()
        )
    );
  }
}
