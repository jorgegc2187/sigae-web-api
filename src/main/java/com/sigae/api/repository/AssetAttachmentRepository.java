package com.sigae.api.repository;

import com.sigae.api.model.entity.AssetAttachment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetAttachmentRepository extends JpaRepository<AssetAttachment, UUID> {
  Optional<AssetAttachment> findByIdAndAssetId(UUID id, UUID assetId);
}
