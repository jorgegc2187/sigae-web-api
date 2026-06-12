package com.sigae.api.repository;

import com.sigae.api.model.entity.AppNotification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

  Optional<AppNotification> findByExternalKey(String externalKey);

  List<AppNotification> findAllByExternalKeyIn(Collection<String> externalKeys);

  @Query("""
      select notification
      from AppNotification notification
      where notification.active = true
        and notification.type in :types
        and notification.externalKey not in :externalKeys
      """)
  List<AppNotification> findActiveManagedNotificationsToDeactivate(
      @Param("types") Collection<com.sigae.api.model.entity.NotificationType> types,
      @Param("externalKeys") Collection<String> externalKeys
  );

  @Query("""
      select notification
      from AppNotification notification
      where (:includeResolved = true or notification.active = true)
        and (:includeAdminOnly = true or notification.adminOnly = false)
        and (:applyLocationScope = false or notification.relatedLocationId in :locationIds)
      order by notification.active desc, notification.occurredAt desc, notification.createdAt desc
      """)
  List<AppNotification> findVisibleForUser(
      @Param("includeResolved") boolean includeResolved,
      @Param("includeAdminOnly") boolean includeAdminOnly,
      @Param("applyLocationScope") boolean applyLocationScope,
      @Param("locationIds") Collection<UUID> locationIds
  );
}
