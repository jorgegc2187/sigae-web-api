package com.sigae.api.repository;

import com.sigae.api.model.entity.UserMfaSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMfaSettingsRepository extends JpaRepository<UserMfaSettings, UUID> {

  Optional<UserMfaSettings> findByUser_Id(UUID userId);
}
