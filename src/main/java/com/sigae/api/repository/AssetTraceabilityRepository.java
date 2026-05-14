package com.sigae.api.repository;

import com.sigae.api.model.entity.AssetTraceability;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetTraceabilityRepository extends JpaRepository<AssetTraceability, UUID> {
  @EntityGraph(attributePaths = {"asset", "user"})
  List<AssetTraceability> findByAssetIdOrderByOccurredAtDesc(UUID assetId);
}
