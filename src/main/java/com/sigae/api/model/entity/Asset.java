package com.sigae.api.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, length = 160)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_type_id", nullable = false)
  private AssetType assetType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "location_id", nullable = false)
  private Location location;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id")
  private Supplier supplier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AssetCondition condition;

  @Column(length = 100)
  private String serialNumber;

  @Column(unique = true, length = 100)
  private String barcode;

  @Column
  private LocalDate acquisitionDate;

  @Column(columnDefinition = "text")
  private String notes;

  @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<AssetAttributeValue> attributeValues = new ArrayList<>();

  @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private Set<AssetAttachment> attachments = new LinkedHashSet<>();

  protected Asset() {}

  public Asset(String code, String name, AssetType assetType, Location location, Supplier supplier, AssetCondition condition) {
    this.code = code;
    this.name = name;
    this.assetType = assetType;
    this.location = location;
    this.supplier = supplier;
    this.condition = condition;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AssetType getAssetType() {
    return assetType;
  }

  public void setAssetType(AssetType assetType) {
    this.assetType = assetType;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  public AssetCondition getCondition() {
    return condition;
  }

  public void setCondition(AssetCondition condition) {
    this.condition = condition;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public LocalDate getAcquisitionDate() {
    return acquisitionDate;
  }

  public void setAcquisitionDate(LocalDate acquisitionDate) {
    this.acquisitionDate = acquisitionDate;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<AssetAttributeValue> getAttributeValues() {
    return attributeValues;
  }

  public void replaceAttributeValues(List<AssetAttributeValue> values) {
    attributeValues.clear();
    attributeValues.addAll(values);
    attributeValues.forEach(value -> value.setAsset(this));
  }

  public void syncAttributeValues(List<AssetAttributeValue> values) {
    Map<UUID, AssetAttributeValue> incomingByDefinitionId = values.stream()
        .collect(Collectors.toMap(
            value -> value.getAttributeDefinition().getId(),
            Function.identity(),
            (left, right) -> right
        ));

    Iterator<AssetAttributeValue> iterator = attributeValues.iterator();
    while (iterator.hasNext()) {
      AssetAttributeValue existingValue = iterator.next();
      AssetAttributeValue incomingValue = incomingByDefinitionId.remove(existingValue.getAttributeDefinition().getId());

      if (incomingValue == null) {
        iterator.remove();
        continue;
      }

      existingValue.setValue(incomingValue.getValue());
    }

    incomingByDefinitionId.values().forEach(value -> {
      value.setAsset(this);
      attributeValues.add(value);
    });
  }

  public Set<AssetAttachment> getAttachments() {
    return attachments;
  }

  public void addAttachment(AssetAttachment attachment) {
    attachments.add(attachment);
    attachment.setAsset(this);
  }
}
