package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "asset_attachment")
public class AssetAttachment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(nullable = false, length = 120)
  private String mimeType;

  @Column(nullable = false)
  private long sizeBytes;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "content", nullable = false, columnDefinition = "bytea")
  private byte[] content;

  protected AssetAttachment() {}

  public AssetAttachment(String fileName, String mimeType, long sizeBytes, byte[] content) {
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.content = content;
  }

  void setAsset(Asset asset) {
    this.asset = asset;
  }

  public Asset getAsset() {
    return asset;
  }

  public String getFileName() {
    return fileName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public byte[] getContent() {
    return content;
  }
}
