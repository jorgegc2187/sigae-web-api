package com.sigae.api.model.dto;

public enum LoanStatusResponse {
  ACTIVO("Activo"),
  VENCIDO("Vencido"),
  DEVUELTO("Devuelto");

  private final String label;

  LoanStatusResponse(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
