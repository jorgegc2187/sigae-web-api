package com.sigae.api.model.dto;

import org.springframework.http.MediaType;

public record ReportExportFile(
    String filename,
    MediaType contentType,
    byte[] content
) {}
