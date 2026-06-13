package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Loan;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LoanSummaryResponse(
    UUID id,
    String code,
    LoanTeacherResponse teacher,
    List<LoanAssetResponse> assets,
    String destination,
    LocalDate loanDate,
    LocalDate dueDate,
    String createdByName,
    String status
) {
  public static LoanSummaryResponse from(Loan loan, LoanStatusResponse status) {
    return new LoanSummaryResponse(
        loan.getId(),
        loan.getCode(),
        LoanResponseMapper.teacherFrom(loan),
        loan.getAssets().stream().map(LoanResponseMapper::assetFrom).toList(),
        loan.getDestinationNameSnapshot(),
        loan.getLoanDate(),
        loan.getDueDate(),
        loan.getCreatedBy() == null ? null : loan.getCreatedBy().getFullName(),
        status.getLabel()
    );
  }
}
