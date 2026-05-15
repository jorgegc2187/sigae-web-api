package com.sigae.api.model.dto;

import com.sigae.api.model.entity.LoanAttachment;
import java.util.UUID;

public record LoanAttachmentResponse(
    UUID id,
    String fileName,
    String mimeType,
    long sizeBytes,
    String source,
    String downloadUrl
) {
  public static LoanAttachmentResponse from(LoanAttachment attachment) {
    return new LoanAttachmentResponse(
        attachment.getId(),
        attachment.getFileName(),
        attachment.getMimeType(),
        attachment.getSizeBytes(),
        attachment.getSource().getValue(),
        "/api/loans/%s/attachments/%s".formatted(attachment.getLoan().getId(), attachment.getId())
    );
  }
}
