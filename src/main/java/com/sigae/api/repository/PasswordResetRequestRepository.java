package com.sigae.api.repository;

import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.model.entity.PasswordResetPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, UUID> {

  Optional<PasswordResetRequest> findByTokenHash(String tokenHash);

  List<PasswordResetRequest> findAllByUser_IdAndPurposeAndUsedAtIsNullAndCancelledAtIsNull(
      UUID userId,
      PasswordResetPurpose purpose
  );

  Optional<PasswordResetRequest> findTopByUser_IdAndPurposeOrderByCreatedAtDesc(
      UUID userId,
      PasswordResetPurpose purpose
  );
}
