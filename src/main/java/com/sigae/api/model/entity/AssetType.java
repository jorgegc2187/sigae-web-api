package com.sigae.api.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asset_type")
public class AssetType extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 80)
  private String icon;

  @OneToMany(mappedBy = "assetType", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<AssetAttributeDefinition> attributes = new ArrayList<>();

  protected AssetType() {}

  public AssetType(String name, String icon) {
    this.name = name;
    this.icon = icon;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public List<AssetAttributeDefinition> getAttributes() {
    return attributes;
  }

  public void replaceAttributes(List<AssetAttributeDefinition> attributes) {
    this.attributes.clear();
    this.attributes.addAll(attributes);
    this.attributes.forEach(attribute -> attribute.setAssetType(this));
  }
}
