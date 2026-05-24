package com.sigae.api.repository;

import com.sigae.api.model.entity.Asset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition"})
  Optional<Asset> findByCodeIgnoreCase(String code);

  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition"})
  Optional<Asset> findByBarcodeIgnoreCase(String barcode);

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition"})
  java.util.List<Asset> findAll();

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition"})
  Optional<Asset> findById(UUID id);

  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier"})
  List<Asset> findAll(Specification<Asset> specification, Sort sort);
}
