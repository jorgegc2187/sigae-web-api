package com.sigae.api.repository;

import com.sigae.api.model.entity.AuthRateLimitBucket;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRateLimitBucketRepository extends JpaRepository<AuthRateLimitBucket, UUID> {

  Optional<AuthRateLimitBucket> findByScopeAndSubjectHashAndClientHash(
      String scope,
      String subjectHash,
      String clientHash
  );
}
