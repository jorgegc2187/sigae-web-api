package com.sigae.api.model.dto;

import org.springframework.http.MediaType;

public record LoanAttachmentFile(
    String filename,
    MediaType contentType,
    byte[] content
) {}
