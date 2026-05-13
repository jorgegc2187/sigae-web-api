package com.sigae.api.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
public class Category extends BaseEntity {

  @Column(nullable = false, unique = true, length = 120)
  private String name;

  @Column(nullable = false, length = 80)
  private String icon;

  @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<AssetType> types = new ArrayList<>();

  protected Category() {}

  public Category(String name, String icon) {
    this.name = name;
    this.icon = icon;
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

  public List<AssetType> getTypes() {
    return types;
  }

  public void addType(AssetType type) {
    types.add(type);
    type.setCategory(this);
  }
}
