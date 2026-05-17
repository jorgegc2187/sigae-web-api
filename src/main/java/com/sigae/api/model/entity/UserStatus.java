package com.sigae.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserStatus {
  ACTIVE("Activo"),
  INACTIVE("Inactivo"),
  PENDING("Pendiente");

  private final String label;

  UserStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static UserStatus fromValue(String value) {
    for (UserStatus status : values()) {
      if (status.name().equalsIgnoreCase(value) || status.label.equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Estado no válido: " + value);
  }
}
