package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_mfa_settings")
public class UserMfaSettings extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  private boolean mfaRequired;

  @Column(nullable = false)
  private boolean mfaEnabled;

  @Column
  private String totpSecretEncrypted;

  @Column
  private Instant mfaEnabledAt;

  protected UserMfaSettings() {}

  public UserMfaSettings(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public boolean isMfaRequired() {
    return mfaRequired;
  }

  public void setMfaRequired(boolean mfaRequired) {
    this.mfaRequired = mfaRequired;
  }

  public boolean isMfaEnabled() {
    return mfaEnabled;
  }

  public void enable(String totpSecretEncrypted) {
    this.mfaEnabled = true;
    this.totpSecretEncrypted = totpSecretEncrypted;
    this.mfaEnabledAt = Instant.now();
  }

  public void reset() {
    this.mfaRequired = false;
    resetEnrollmentOnly();
  }

  public void resetEnrollmentOnly() {
    this.mfaEnabled = false;
    this.totpSecretEncrypted = null;
    this.mfaEnabledAt = null;
  }

  public String getTotpSecretEncrypted() {
    return totpSecretEncrypted;
  }

  public Instant getMfaEnabledAt() {
    return mfaEnabledAt;
  }
}
