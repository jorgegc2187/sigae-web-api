package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetAttachment;
import java.util.UUID;

public record AssetAttachmentResponse(
    UUID id,
    String fileName,
    String mimeType,
    long sizeBytes,
    String downloadUrl
) {
  public static AssetAttachmentResponse from(AssetAttachment attachment) {
    return new AssetAttachmentResponse(
        attachment.getId(),
        attachment.getFileName(),
        attachment.getMimeType(),
        attachment.getSizeBytes(),
        "/api/assets/%s/attachments/%s".formatted(attachment.getAsset().getId(), attachment.getId())
    );
  }
}
