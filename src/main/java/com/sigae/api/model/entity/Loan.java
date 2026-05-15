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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "loan")
public class Loan extends BaseEntity {

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_id", nullable = false)
  private Teacher teacher;

  @Column(nullable = false, length = 160)
  private String teacherNameSnapshot;

  @Column(nullable = false, length = 8)
  private String teacherDniSnapshot;

  @Column(length = 120)
  private String teacherSpecialtySnapshot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_location_id", nullable = false)
  private Location destinationLocation;

  @Column(nullable = false, length = 150)
  private String destinationNameSnapshot;

  @Column(nullable = false)
  private LocalDate loanDate;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Column
  private Instant completedAt;

  @Column(columnDefinition = "text")
  private String notes;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "signature_png", columnDefinition = "bytea")
  private byte[] signaturePng;

  @Column(length = 80)
  private String signatureContentType;

  @Column(length = 180)
  private String signatureFileName;

  @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<LoanAsset> assets = new ArrayList<>();

  @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<LoanAttachment> attachments = new ArrayList<>();

  protected Loan() {}

  public Loan(
      String code,
      Teacher teacher,
      Location destinationLocation,
      LocalDate loanDate,
      LocalDate dueDate,
      String notes
  ) {
    this.code = code;
    this.teacher = teacher;
    this.teacherNameSnapshot = teacher.getFullName();
    this.teacherDniSnapshot = teacher.getDni();
    this.teacherSpecialtySnapshot = teacher.getSpecialty();
    this.destinationLocation = destinationLocation;
    this.destinationNameSnapshot = destinationLocation.getName();
    this.loanDate = loanDate;
    this.dueDate = dueDate;
    this.notes = notes;
  }

  public String getCode() {
    return code;
  }

  public Teacher getTeacher() {
    return teacher;
  }

  public String getTeacherNameSnapshot() {
    return teacherNameSnapshot;
  }

  public String getTeacherDniSnapshot() {
    return teacherDniSnapshot;
  }

  public String getTeacherSpecialtySnapshot() {
    return teacherSpecialtySnapshot;
  }

  public Location getDestinationLocation() {
    return destinationLocation;
  }

  public String getDestinationNameSnapshot() {
    return destinationNameSnapshot;
  }

  public LocalDate getLoanDate() {
    return loanDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void markReturned() {
    this.completedAt = Instant.now();
  }

  public String getNotes() {
    return notes;
  }

  public byte[] getSignaturePng() {
    return signaturePng;
  }

  public String getSignatureContentType() {
    return signatureContentType;
  }

  public String getSignatureFileName() {
    return signatureFileName;
  }

  public void setSignature(byte[] signaturePng, String signatureContentType, String signatureFileName) {
    this.signaturePng = signaturePng;
    this.signatureContentType = signatureContentType;
    this.signatureFileName = signatureFileName;
  }

  public List<LoanAsset> getAssets() {
    return assets;
  }

  public List<LoanAttachment> getAttachments() {
    return attachments;
  }

  public void addAsset(LoanAsset loanAsset) {
    assets.add(loanAsset);
    loanAsset.setLoan(this);
  }

  public void addAttachment(LoanAttachment attachment) {
    attachments.add(attachment);
    attachment.setLoan(this);
  }
}
