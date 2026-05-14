package com.sigae.api.repository;

import com.sigae.api.model.entity.Supplier;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
  Optional<Supplier> findByNameIgnoreCase(String name);
  Optional<Supplier> findByRuc(String ruc);
}
