package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @Column
  private Instant usedAt;

  protected PasswordResetRequest() {}

  public PasswordResetRequest(User user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
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

  public Instant getUsedAt() {
    return usedAt;
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public boolean isExpired() {
    return expiresAt.isBefore(Instant.now());
  }

  public boolean isActive() {
    return !isUsed() && !isExpired();
  }

  public void markUsed() {
    usedAt = Instant.now();
  }
}
