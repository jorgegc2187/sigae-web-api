package com.sigae.api.repository;

import com.sigae.api.model.entity.MfaChallenge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

  Optional<MfaChallenge> findByTokenHash(String tokenHash);

  List<MfaChallenge> findAllByUser_IdAndConsumedAtIsNull(UUID userId);
}
