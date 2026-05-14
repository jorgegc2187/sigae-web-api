package com.sigae.api.repository;

import com.sigae.api.model.entity.Asset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
  Optional<Asset> findByCodeIgnoreCase(String code);
  Optional<Asset> findByBarcodeIgnoreCase(String barcode);

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition"})
  java.util.List<Asset> findAll();

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition"})
  Optional<Asset> findById(UUID id);
}
