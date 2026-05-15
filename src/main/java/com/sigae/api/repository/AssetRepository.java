package com.sigae.api.repository;

import com.sigae.api.model.entity.Asset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
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
  @Query("""
      select asset
      from Asset asset
      where (:categoryId is null or asset.assetType.category.id = :categoryId)
        and (:locationId is null or asset.location.id = :locationId)
        and (:startDate is null or asset.acquisitionDate >= :startDate)
        and (:endDate is null or asset.acquisitionDate <= :endDate)
      order by asset.code asc
      """)
  List<Asset> findAssetsReport(
      @Param("categoryId") UUID categoryId,
      @Param("locationId") UUID locationId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );
}
