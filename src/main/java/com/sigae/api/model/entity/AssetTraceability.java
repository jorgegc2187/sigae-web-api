package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "asset_traceability")
public class AssetTraceability extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TraceabilityEventType eventType;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(columnDefinition = "text")
  private String previousValue;

  @Column(columnDefinition = "text")
  private String newValue;

  @Column(columnDefinition = "text")
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private Instant occurredAt;

  protected AssetTraceability() {}

  public AssetTraceability(
      Asset asset,
      TraceabilityEventType eventType,
      String description,
      String previousValue,
      String newValue,
      String reason,
      User user
  ) {
    this.asset = asset;
    this.eventType = eventType;
    this.description = description;
    this.previousValue = previousValue;
    this.newValue = newValue;
    this.reason = reason;
    this.user = user;
  }

  @PrePersist
  void setOccurredAt() {
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }

  public Asset getAsset() {
    return asset;
  }

  public TraceabilityEventType getEventType() {
    return eventType;
  }

  public String getDescription() {
    return description;
  }

  public String getPreviousValue() {
    return previousValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getReason() {
    return reason;
  }

  public User getUser() {
    return user;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
