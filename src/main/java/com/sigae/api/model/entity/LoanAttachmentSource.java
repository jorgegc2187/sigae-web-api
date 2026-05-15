package com.sigae.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LoanAttachmentSource {
  PICKER("picker"),
  CAMERA("camera"),
  GALLERY("gallery"),
  FILES("files");

  private final String value;

  LoanAttachmentSource(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static LoanAttachmentSource fromValue(String value) {
    if (value == null || value.isBlank()) {
      return PICKER;
    }

    for (LoanAttachmentSource source : values()) {
      if (source.name().equalsIgnoreCase(value) || source.value.equalsIgnoreCase(value)) {
        return source;
      }
    }

    return PICKER;
  }
}
