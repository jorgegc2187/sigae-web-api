package com.sigae.api.model.dto;

import com.sigae.api.exception.BadRequestException;
import org.springframework.http.MediaType;

public enum ReportExportFormat {
  PDF("pdf", "application/pdf"),
  EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
  WORD("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

  private final String extension;
  private final MediaType contentType;

  ReportExportFormat(String extension, String contentType) {
    this.extension = extension;
    this.contentType = MediaType.parseMediaType(contentType);
  }

  public String extension() {
    return extension;
  }

  public MediaType contentType() {
    return contentType;
  }

  public static ReportExportFormat from(String value) {
    if (value == null || value.isBlank()) {
      return PDF;
    }

    for (ReportExportFormat format : values()) {
      if (format.name().equalsIgnoreCase(value) || format.extension.equalsIgnoreCase(value)) {
        return format;
      }
    }

    throw new BadRequestException("Formato de reporte no soportado.");
  }
}
