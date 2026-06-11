package com.sigae.api.repository;

import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.TraceabilityEventType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetTraceabilityRepository extends JpaRepository<AssetTraceability, UUID> {
  interface AssetEventOccurredAtView {
    UUID getAssetId();
    Instant getOccurredAt();
  }

  @EntityGraph(attributePaths = {"asset", "user", "attachments"})
  List<AssetTraceability> findByAssetIdOrderByOccurredAtDesc(UUID assetId);

  @EntityGraph(attributePaths = {"asset", "user", "attachments"})
  List<AssetTraceability> findByAssetIdInOrderByOccurredAtDesc(Collection<UUID> assetIds);

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

  @Query("""
      select traceability.asset.id as assetId, max(traceability.occurredAt) as occurredAt
      from AssetTraceability traceability
      where traceability.asset.id in :assetIds
        and traceability.eventType = :eventType
      group by traceability.asset.id
      """)
  List<AssetEventOccurredAtView> findLatestOccurredAtByAssetIdsAndEventType(
      @Param("assetIds") Collection<UUID> assetIds,
      @Param("eventType") TraceabilityEventType eventType
  );
}
