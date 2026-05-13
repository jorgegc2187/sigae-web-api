package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_attribute_definition")
public class AssetAttributeDefinition extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_type_id", nullable = false)
  private AssetType assetType;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 255)
  private String description;

  @Column(nullable = false)
  private boolean isRequired;

  protected AssetAttributeDefinition() {}

  public AssetAttributeDefinition(String name, String description, boolean isRequired) {
    this.name = name;
    this.description = description;
    this.isRequired = isRequired;
  }

  public void setAssetType(AssetType assetType) {
    this.assetType = assetType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public void setRequired(boolean required) {
    isRequired = required;
  }
}
