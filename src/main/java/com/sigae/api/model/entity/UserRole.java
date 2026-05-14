package com.sigae.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
  ADMINISTRADOR("Administrador"),
  ENCARGADO("Encargado"),
  SOLO_LECTURA("Solo Lectura");

  private final String label;

  UserRole(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static UserRole fromValue(String value) {
    for (UserRole role : values()) {
      if (role.name().equalsIgnoreCase(value) || role.label.equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Rol no válido: " + value);
  }
}
