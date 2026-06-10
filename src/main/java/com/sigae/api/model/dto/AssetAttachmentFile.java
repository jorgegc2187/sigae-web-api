package com.sigae.api.model.dto;

import org.springframework.http.MediaType;

public record AssetAttachmentFile(
    String filename,
    MediaType mediaType,
    byte[] content
) {}
