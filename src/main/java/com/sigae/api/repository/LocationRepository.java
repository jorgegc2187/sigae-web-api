package com.sigae.api.repository;

import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.CatalogStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, UUID> {
  Optional<Location> findByNameIgnoreCase(String name);
  List<Location> findAllByStatus(CatalogStatus status);
}
