package com.sigae.api.repository;

import com.sigae.api.model.entity.AssetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetTypeRepository extends JpaRepository<AssetType, UUID> {

  boolean existsByCategoryIdAndNameIgnoreCase(UUID categoryId, String name);

  @EntityGraph(attributePaths = {"category", "attributes"})
  Optional<AssetType> findWithCategoryAndAttributesById(UUID id);
}
