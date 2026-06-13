package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "auth_rate_limit_bucket",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_auth_rate_limit_bucket_scope_subject_client",
        columnNames = {"scope", "subject_hash", "client_hash"}
    )
)
public class AuthRateLimitBucket extends BaseEntity {

  @Column(nullable = false, length = 80)
  private String scope;

  @Column(nullable = false, length = 64)
  private String subjectHash;

  @Column(nullable = false, length = 64)
  private String clientHash;

  @Column(nullable = false)
  private int requestCount;

  @Column(nullable = false)
  private Instant windowStartedAt;

  @Column
  private Instant blockedUntil;

  protected AuthRateLimitBucket() {}

  public AuthRateLimitBucket(String scope, String subjectHash, String clientHash, Instant windowStartedAt) {
    this.scope = scope;
    this.subjectHash = subjectHash;
    this.clientHash = clientHash;
    this.windowStartedAt = windowStartedAt;
    this.requestCount = 0;
  }

  public String getScope() {
    return scope;
  }

  public String getSubjectHash() {
    return subjectHash;
  }

  public String getClientHash() {
    return clientHash;
  }

  public int getRequestCount() {
    return requestCount;
  }

  public void setRequestCount(int requestCount) {
    this.requestCount = requestCount;
  }

  public Instant getWindowStartedAt() {
    return windowStartedAt;
  }

  public void setWindowStartedAt(Instant windowStartedAt) {
    this.windowStartedAt = windowStartedAt;
  }

  public Instant getBlockedUntil() {
    return blockedUntil;
  }

  public void setBlockedUntil(Instant blockedUntil) {
    this.blockedUntil = blockedUntil;
  }
}
