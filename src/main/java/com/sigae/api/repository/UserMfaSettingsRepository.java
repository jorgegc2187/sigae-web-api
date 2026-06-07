package com.sigae.api.repository;

import com.sigae.api.model.entity.UserMfaSettings;
import com.sigae.api.model.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMfaSettingsRepository extends JpaRepository<UserMfaSettings, UUID> {

  Optional<UserMfaSettings> findByUser_Id(UUID userId);

  @Query("""
      select settings
      from UserMfaSettings settings
      join fetch settings.user user
      where settings.mfaRequired = true
        and settings.mfaEnabled = false
        and user.status = :userStatus
      order by settings.updatedAt desc
      """)
  List<UserMfaSettings> findPendingForLiveNotifications(@Param("userStatus") UserStatus userStatus);
}
