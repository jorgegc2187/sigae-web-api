package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_notification")
public class AppNotification extends BaseEntity {

  @Column(nullable = false, length = 160, unique = true)
  private String externalKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  private NotificationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationSeverity severity;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String message;

  @Column(nullable = false, length = 255)
  private String route;

  @Column(nullable = false)
  private Instant occurredAt;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean adminOnly = false;

  @Column
  private UUID relatedLocationId;

  protected AppNotification() {}

  public AppNotification(
      String externalKey,
      NotificationType type,
      NotificationSeverity severity,
      String title,
      String message,
      String route,
      Instant occurredAt,
      boolean adminOnly,
      UUID relatedLocationId
  ) {
    this.externalKey = externalKey;
    this.type = type;
    this.severity = severity;
    this.title = title;
    this.message = message;
    this.route = route;
    this.occurredAt = occurredAt;
    this.adminOnly = adminOnly;
    this.relatedLocationId = relatedLocationId;
    this.active = true;
  }

  public String getExternalKey() {
    return externalKey;
  }

  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public NotificationSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(NotificationSeverity severity) {
    this.severity = severity;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getRoute() {
    return route;
  }

  public void setRoute(String route) {
    this.route = route;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isAdminOnly() {
    return adminOnly;
  }

  public void setAdminOnly(boolean adminOnly) {
    this.adminOnly = adminOnly;
  }

  public UUID getRelatedLocationId() {
    return relatedLocationId;
  }

  public void setRelatedLocationId(UUID relatedLocationId) {
    this.relatedLocationId = relatedLocationId;
  }
}
