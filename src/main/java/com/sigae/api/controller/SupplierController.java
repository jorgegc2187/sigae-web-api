package com.sigae.api.controller;

import com.sigae.api.model.dto.SupplierRequest;
import com.sigae.api.model.dto.SupplierResponse;
import com.sigae.api.service.SupplierService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

  private final SupplierService supplierService;

  public SupplierController(SupplierService supplierService) {
    this.supplierService = supplierService;
  }

  @GetMapping
  public List<SupplierResponse> list() {
    return supplierService.findAll().stream().map(SupplierResponse::from).toList();
  }

  @GetMapping("/{supplierId}")
  public SupplierResponse getById(@PathVariable UUID supplierId) {
    return SupplierResponse.from(supplierService.getById(supplierId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
    return SupplierResponse.from(supplierService.create(request));
  }

  @PatchMapping("/{supplierId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public SupplierResponse update(
      @PathVariable UUID supplierId,
      @Valid @RequestBody SupplierRequest request
  ) {
    return SupplierResponse.from(supplierService.update(supplierId, request));
  }

  @PatchMapping("/{supplierId}/deactivate")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public SupplierResponse deactivate(@PathVariable UUID supplierId) {
    return SupplierResponse.from(supplierService.deactivate(supplierId));
  }
}
