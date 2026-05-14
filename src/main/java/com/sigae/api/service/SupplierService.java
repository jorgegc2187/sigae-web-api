package com.sigae.api.service;

import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.SupplierRequest;
import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Supplier;
import com.sigae.api.repository.SupplierRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SupplierService {

  private final SupplierRepository supplierRepository;

  public SupplierService(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  public List<Supplier> findAll() {
    return supplierRepository.findAll();
  }

  public Supplier getById(UUID id) {
    return supplierRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Proveedor no encontrado."));
  }

  @Transactional
  public Supplier create(SupplierRequest request) {
    ensureAvailable(request.name(), request.ruc(), null);
    return supplierRepository.save(new Supplier(
        request.name().trim(),
        normalizeOptional(request.ruc()),
        normalizeOptional(request.email()),
        normalizeOptional(request.phone()),
        normalizeOptional(request.address()),
        request.status()
    ));
  }

  @Transactional
  public Supplier update(UUID id, SupplierRequest request) {
    Supplier supplier = getById(id);
    ensureAvailable(request.name(), request.ruc(), supplier.getId());
    supplier.setName(request.name().trim());
    supplier.setRuc(normalizeOptional(request.ruc()));
    supplier.setEmail(normalizeOptional(request.email()));
    supplier.setPhone(normalizeOptional(request.phone()));
    supplier.setAddress(normalizeOptional(request.address()));
    supplier.setStatus(request.status());
    return supplierRepository.save(supplier);
  }

  @Transactional
  public Supplier deactivate(UUID id) {
    Supplier supplier = getById(id);
    supplier.setStatus(CatalogStatus.INACTIVE);
    return supplierRepository.save(supplier);
  }

  private void ensureAvailable(String name, String ruc, UUID currentId) {
    supplierRepository.findByNameIgnoreCase(name.trim())
        .filter(supplier -> !supplier.getId().equals(currentId))
        .ifPresent(supplier -> {
          throw new ConflictException("Ya existe un proveedor con ese nombre.");
        });

    String normalizedRuc = normalizeOptional(ruc);
    if (normalizedRuc != null) {
      supplierRepository.findByRuc(normalizedRuc)
          .filter(supplier -> !supplier.getId().equals(currentId))
          .ifPresent(supplier -> {
            throw new ConflictException("Ya existe un proveedor con ese RUC.");
          });
    }
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
