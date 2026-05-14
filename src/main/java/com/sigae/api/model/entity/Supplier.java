package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier")
public class Supplier extends BaseEntity {

  @Column(nullable = false, unique = true, length = 150)
  private String name;

  @Column(unique = true, length = 11)
  private String ruc;

  @Column(length = 150)
  private String email;

  @Column(length = 20)
  private String phone;

  @Column(columnDefinition = "text")
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private CatalogStatus status;

  protected Supplier() {}

  public Supplier(String name, String ruc, String email, String phone, String address, CatalogStatus status) {
    this.name = name;
    this.ruc = ruc;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRuc() {
    return ruc;
  }

  public void setRuc(String ruc) {
    this.ruc = ruc;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public CatalogStatus getStatus() {
    return status;
  }

  public void setStatus(CatalogStatus status) {
    this.status = status;
  }
}
