package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_traceability_attachment")
public class AssetTraceabilityAttachment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "traceability_id", nullable = false)
  private AssetTraceability traceability;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(nullable = false, length = 120)
  private String mimeType;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false, columnDefinition = "bytea")
  private byte[] content;

  protected AssetTraceabilityAttachment() {}

  public AssetTraceabilityAttachment(String fileName, String mimeType, long sizeBytes, byte[] content) {
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.content = content;
  }

  public AssetTraceability getTraceability() {
    return traceability;
  }

  public void setTraceability(AssetTraceability traceability) {
    this.traceability = traceability;
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
