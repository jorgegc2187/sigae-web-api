package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Loan;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public record LoanDetailResponse(
    UUID id,
    String code,
    LoanTeacherResponse teacher,
    List<LoanAssetResponse> assets,
    String destination,
    LocalDate loanDate,
    LocalDate dueDate,
    Instant completedDate,
    String status,
    String createdByName,
    String notes,
    String signatureDataUrl,
    List<LoanAttachmentResponse> attachments,
    List<LoanActivityResponse> activities
) {
  public static LoanDetailResponse from(
      Loan loan,
      LoanStatusResponse status,
      List<LoanActivityResponse> activities
  ) {
    return new LoanDetailResponse(
        loan.getId(),
        loan.getCode(),
        LoanResponseMapper.teacherFrom(loan),
        loan.getAssets().stream().map(LoanResponseMapper::assetFrom).toList(),
        loan.getDestinationNameSnapshot(),
        loan.getLoanDate(),
        loan.getDueDate(),
        loan.getCompletedAt(),
        status.getLabel(),
        loan.getCreatedBy() == null ? null : loan.getCreatedBy().getFullName(),
        loan.getNotes(),
        signatureDataUrl(loan),
        loan.getAttachments().stream().map(LoanAttachmentResponse::from).toList(),
        activities
    );
  }

  private static String signatureDataUrl(Loan loan) {
    if (loan.getSignaturePng() == null || loan.getSignaturePng().length == 0) {
      return null;
    }

    String contentType = loan.getSignatureContentType() == null ? "image/png" : loan.getSignatureContentType();
    return "data:%s;base64,%s".formatted(contentType, Base64.getEncoder().encodeToString(loan.getSignaturePng()));
  }
}
