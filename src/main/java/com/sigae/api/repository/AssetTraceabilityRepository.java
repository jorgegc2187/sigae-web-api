package com.sigae.api.repository;

import com.sigae.api.model.entity.AssetTraceability;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetTraceabilityRepository extends JpaRepository<AssetTraceability, UUID> {
  @EntityGraph(attributePaths = {"asset", "user"})
  List<AssetTraceability> findByAssetIdOrderByOccurredAtDesc(UUID assetId);

  @Query("""
      select traceability
      from AssetTraceability traceability
      join fetch traceability.asset asset
      join fetch asset.assetType assetType
      join fetch assetType.category category
      where (:applyScope = false or asset.location.id in :locationIds)
      order by traceability.occurredAt desc
      """)
  List<AssetTraceability> findRecentForDashboard(
      @Param("applyScope") boolean applyScope,
      @Param("locationIds") Collection<UUID> locationIds,
      Pageable pageable
  );
}
