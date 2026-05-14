package com.sigae.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetCondition {
  BUENO("Bueno"),
  REGULAR("Regular"),
  MALO("Malo"),
  MANTENIMIENTO("Mantenimiento"),
  DADO_DE_BAJA("Dado de baja");

  private final String label;

  AssetCondition(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static AssetCondition fromValue(String value) {
    String normalized = value.replace(' ', '_');
    for (AssetCondition condition : values()) {
      if (condition.name().equalsIgnoreCase(normalized) || condition.label.equalsIgnoreCase(value)) {
        return condition;
      }
    }
    throw new IllegalArgumentException("Condición no válida: " + value);
  }
}
