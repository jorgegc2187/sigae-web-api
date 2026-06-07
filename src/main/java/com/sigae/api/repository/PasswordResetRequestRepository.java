package com.sigae.api.repository;

import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Query("""
      select request
      from PasswordResetRequest request
      join fetch request.user user
      where request.purpose = :purpose
        and user.status = :userStatus
        and request.usedAt is null
        and request.cancelledAt is null
        and request.expiresAt > :now
      order by request.createdAt desc
      """)
  List<PasswordResetRequest> findActiveForLiveNotifications(
      @Param("purpose") PasswordResetPurpose purpose,
      @Param("userStatus") UserStatus userStatus,
      @Param("now") Instant now
  );
}
