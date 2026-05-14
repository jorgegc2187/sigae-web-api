package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher")
public class Teacher extends BaseEntity {

  @Column(nullable = false, unique = true, length = 8)
  private String dni;

  @Column(nullable = false, length = 160)
  private String fullName;

  @Column(length = 120)
  private String specialty;

  @Column(length = 150)
  private String email;

  @Column(length = 20)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private CatalogStatus status;

  protected Teacher() {}

  public Teacher(String dni, String fullName, String specialty, String email, String phone, CatalogStatus status) {
    this.dni = dni;
    this.fullName = fullName;
    this.specialty = specialty;
    this.email = email;
    this.phone = phone;
    this.status = status;
  }

  public String getDni() {
    return dni;
  }

  public void setDni(String dni) {
    this.dni = dni;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getSpecialty() {
    return specialty;
  }

  public void setSpecialty(String specialty) {
    this.specialty = specialty;
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

  public CatalogStatus getStatus() {
    return status;
  }

  public void setStatus(CatalogStatus status) {
    this.status = status;
  }
}
