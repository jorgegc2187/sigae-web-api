package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "location")
public class Location extends BaseEntity {

  @Column(nullable = false, unique = true, length = 150)
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private CatalogStatus status;

  protected Location() {}

  public Location(String name, String description, CatalogStatus status) {
    this.name = name;
    this.description = description;
    this.status = status;
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

  public CatalogStatus getStatus() {
    return status;
  }

  public void setStatus(CatalogStatus status) {
    this.status = status;
  }
}
