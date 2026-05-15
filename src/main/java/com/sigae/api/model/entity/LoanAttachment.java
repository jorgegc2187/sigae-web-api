package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "loan_attachment")
public class LoanAttachment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loan_id", nullable = false)
  private Loan loan;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(nullable = false, length = 120)
  private String mimeType;

  @Column(nullable = false)
  private long sizeBytes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private LoanAttachmentSource source;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "content", nullable = false, columnDefinition = "bytea")
  private byte[] content;

  protected LoanAttachment() {}

  public LoanAttachment(String fileName, String mimeType, long sizeBytes, LoanAttachmentSource source, byte[] content) {
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.source = source;
    this.content = content;
  }

  void setLoan(Loan loan) {
    this.loan = loan;
  }

  public Loan getLoan() {
    return loan;
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

  public LoanAttachmentSource getSource() {
    return source;
  }

  public byte[] getContent() {
    return content;
  }
}
