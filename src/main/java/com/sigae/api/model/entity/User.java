package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

  @Column(nullable = false, length = 160)
  private String fullName;

  @Column(nullable = false, unique = true, length = 320)
  private String email;

  @Column(nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private UserStatus status;

  @Column
  private Instant lastAccessAt;

  @Column(nullable = false)
  private int failedLoginAttempts;

  @Column
  private Instant firstFailedLoginAt;

  @Column
  private Instant lockedUntil;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_location",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "location_id")
  )
  private Set<Location> locations = new LinkedHashSet<>();

  protected User() {}

  public User(String fullName, String email, String passwordHash, UserRole role, UserStatus status) {
    this.fullName = fullName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.status = status;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public Instant getLastAccessAt() {
    return lastAccessAt;
  }

  public void setLastAccessAt(Instant lastAccessAt) {
    this.lastAccessAt = lastAccessAt;
  }

  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public void setFailedLoginAttempts(int failedLoginAttempts) {
    this.failedLoginAttempts = failedLoginAttempts;
  }

  public Instant getFirstFailedLoginAt() {
    return firstFailedLoginAt;
  }

  public void setFirstFailedLoginAt(Instant firstFailedLoginAt) {
    this.firstFailedLoginAt = firstFailedLoginAt;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(Instant lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public Set<Location> getLocations() {
    return locations;
  }

  public void setLocations(Set<Location> locations) {
    this.locations = new LinkedHashSet<>(locations);
  }
}
