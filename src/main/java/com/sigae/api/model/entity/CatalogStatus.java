package com.sigae.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CatalogStatus {
  ACTIVE("Activo"),
  INACTIVE("Inactivo");

  private final String label;

  CatalogStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static CatalogStatus fromValue(String value) {
    for (CatalogStatus status : values()) {
      if (status.name().equalsIgnoreCase(value) || status.label.equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Estado no válido: " + value);
  }
}
