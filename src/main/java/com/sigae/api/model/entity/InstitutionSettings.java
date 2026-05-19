package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "institution_settings")
public class InstitutionSettings {

  public static final long SINGLETON_ID = 1L;

  @Id
  @Column(nullable = false, updatable = false)
  private Long id;

  @Column(nullable = false, length = 255)
  private String systemName;

  @Column(length = 255)
  private String address;

  @Column(length = 120)
  private String city;

  @Column(length = 60)
  private String supportPhone;

  @Column(nullable = false, length = 255)
  private String supportEmail;

  @Column(length = 255)
  private String logoFileName;

  @Column(length = 120)
  private String logoMimeType;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "logo_content", columnDefinition = "bytea")
  private byte[] logoContent;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected InstitutionSettings() {}

  public InstitutionSettings(
      String systemName,
      String address,
      String city,
      String supportPhone,
      String supportEmail
  ) {
    this.id = SINGLETON_ID;
    this.systemName = systemName;
    this.address = address;
    this.city = city;
    this.supportPhone = supportPhone;
    this.supportEmail = supportEmail;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (id == null) {
      id = SINGLETON_ID;
    }
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getSystemName() {
    return systemName;
  }

  public String getAddress() {
    return address;
  }

  public String getCity() {
    return city;
  }

  public String getSupportPhone() {
    return supportPhone;
  }

  public String getSupportEmail() {
    return supportEmail;
  }

  public String getLogoFileName() {
    return logoFileName;
  }

  public String getLogoMimeType() {
    return logoMimeType;
  }

  public byte[] getLogoContent() {
    return logoContent;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public boolean hasLogo() {
    return logoContent != null && logoContent.length > 0;
  }

  public void update(
      String systemName,
      String address,
      String city,
      String supportPhone,
      String supportEmail
  ) {
    this.systemName = systemName;
    this.address = address;
    this.city = city;
    this.supportPhone = supportPhone;
    this.supportEmail = supportEmail;
  }

  public void updateLogo(String fileName, String mimeType, byte[] content) {
    this.logoFileName = fileName;
    this.logoMimeType = mimeType;
    this.logoContent = content;
  }
}
