package com.sigae.api.repository;

import com.sigae.api.model.entity.AssetTraceabilityAttachment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetTraceabilityAttachmentRepository extends JpaRepository<AssetTraceabilityAttachment, UUID> {
  Optional<AssetTraceabilityAttachment> findByIdAndTraceabilityIdAndTraceabilityAssetId(
      UUID attachmentId,
      UUID traceabilityId,
      UUID assetId
  );
}
