package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Supplier;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    String name,
    String ruc,
    String email,
    String phone,
    String address,
    CatalogStatus status
) {
  public static SupplierResponse from(Supplier supplier) {
    return new SupplierResponse(
        supplier.getId(),
        supplier.getName(),
        supplier.getRuc(),
        supplier.getEmail(),
        supplier.getPhone(),
        supplier.getAddress(),
        supplier.getStatus()
    );
  }
}
