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
@Table(name = "mfa_challenge")
public class MfaChallenge extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true, length = 128)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private MfaChallengePurpose purpose;

  @Column
  private String encryptedTotpSecret;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column
  private Instant consumedAt;

  @Column(nullable = false)
  private int failedAttempts;

  protected MfaChallenge() {}

  public MfaChallenge(User user, String tokenHash, MfaChallengePurpose purpose, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.purpose = purpose;
    this.expiresAt = expiresAt;
  }

  public User getUser() {
    return user;
  }

  public MfaChallengePurpose getPurpose() {
    return purpose;
  }

  public String getEncryptedTotpSecret() {
    return encryptedTotpSecret;
  }

  public void setEncryptedTotpSecret(String encryptedTotpSecret) {
    this.encryptedTotpSecret = encryptedTotpSecret;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public boolean isActive() {
    return consumedAt == null && expiresAt.isAfter(Instant.now());
  }

  public void markConsumed() {
    this.consumedAt = Instant.now();
  }

  public void recordFailure() {
    this.failedAttempts += 1;
  }
}
