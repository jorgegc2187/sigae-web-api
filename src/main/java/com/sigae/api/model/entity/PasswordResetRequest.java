package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "password_reset_request")
public class PasswordResetRequest extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @Column(nullable = false, length = 128)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PasswordResetPurpose purpose;

  @Column
  private Instant usedAt;

  @Column
  private Instant cancelledAt;

  protected PasswordResetRequest() {}

  public PasswordResetRequest(User user, String tokenHash, Instant expiresAt) {
    this(user, tokenHash, expiresAt, PasswordResetPurpose.PASSWORD_RESET);
  }

  public PasswordResetRequest(User user, String tokenHash, Instant expiresAt, PasswordResetPurpose purpose) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.purpose = purpose;
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public PasswordResetPurpose getPurpose() {
    return purpose;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public boolean isCancelled() {
    return cancelledAt != null;
  }

  public boolean isExpired() {
    return expiresAt.isBefore(Instant.now());
  }

  public boolean isActive() {
    return !isUsed() && !isCancelled() && !isExpired();
  }

  public void markUsed() {
    usedAt = Instant.now();
  }

  public void markCancelled() {
    if (cancelledAt == null) {
      cancelledAt = Instant.now();
    }
  }
}
