package com.sigae.api.repository;

import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {
  interface AssetConditionCountView {
    AssetCondition getCondition();
    long getTotal();
  }

  interface AssetCategoryCountView {
    UUID getCategoryId();
    String getCategoryName();
    long getTotal();
  }

  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition", "attachments"})
  Optional<Asset> findByCodeIgnoreCase(String code);

  List<Asset> findAllByCodeStartingWithIgnoreCase(String prefix);

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition", "attachments"})
  java.util.List<Asset> findAll();

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition", "attachments"})
  java.util.List<Asset> findAll(Sort sort);

  @Override
  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier", "attributeValues", "attributeValues.attributeDefinition", "attachments"})
  Optional<Asset> findById(UUID id);

  @EntityGraph(attributePaths = {"assetType", "assetType.category", "location", "supplier"})
  List<Asset> findAll(Specification<Asset> specification, Sort sort);

  @Query("""
      select count(asset)
      from Asset asset
      where (:applyScope = false or asset.location.id in :locationIds)
      """)
  long countForDashboard(
      @Param("applyScope") boolean applyScope,
      @Param("locationIds") Collection<UUID> locationIds
  );

  @Query("""
      select asset.condition as condition, count(asset) as total
      from Asset asset
      where (:applyScope = false or asset.location.id in :locationIds)
      group by asset.condition
      """)
  List<AssetConditionCountView> countByConditionForDashboard(
      @Param("applyScope") boolean applyScope,
      @Param("locationIds") Collection<UUID> locationIds
  );

  @Query("""
      select asset.assetType.category.id as categoryId,
             asset.assetType.category.name as categoryName,
             count(asset) as total
      from Asset asset
      where (:applyScope = false or asset.location.id in :locationIds)
      group by asset.assetType.category.id, asset.assetType.category.name
      order by count(asset) desc, lower(asset.assetType.category.name) asc
      """)
  List<AssetCategoryCountView> countByCategoryForDashboard(
      @Param("applyScope") boolean applyScope,
      @Param("locationIds") Collection<UUID> locationIds
  );
}
