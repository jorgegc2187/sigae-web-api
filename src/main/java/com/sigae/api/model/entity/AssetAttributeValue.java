package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_attribute_value")
public class AssetAttributeValue extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attribute_definition_id", nullable = false)
  private AssetAttributeDefinition attributeDefinition;

  @Column(name = "attribute_value", nullable = false, columnDefinition = "text")
  private String value;

  protected AssetAttributeValue() {}

  public AssetAttributeValue(AssetAttributeDefinition attributeDefinition, String value) {
    this.attributeDefinition = attributeDefinition;
    this.value = value;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  public AssetAttributeDefinition getAttributeDefinition() {
    return attributeDefinition;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
