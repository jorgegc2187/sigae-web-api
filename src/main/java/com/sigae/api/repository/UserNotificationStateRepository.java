package com.sigae.api.repository;

import com.sigae.api.model.entity.UserNotificationState;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationStateRepository extends JpaRepository<UserNotificationState, UUID> {

  Optional<UserNotificationState> findByNotification_IdAndUser_Id(UUID notificationId, UUID userId);

  List<UserNotificationState> findByUser_IdAndNotification_IdIn(UUID userId, Collection<UUID> notificationIds);
}
