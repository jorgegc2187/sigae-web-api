package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Loan;
import java.time.LocalDate;
import java.util.UUID;

public record LoanReportRowResponse(
    UUID id,
    String code,
    String teacherName,
    int assetsCount,
    LocalDate loanDate,
    LocalDate dueDate,
    String location,
    String status
) {
  public static LoanReportRowResponse from(Loan loan, LoanStatusResponse status) {
    return new LoanReportRowResponse(
        loan.getId(),
        loan.getCode(),
        loan.getTeacherNameSnapshot(),
        loan.getAssets().size(),
        loan.getLoanDate(),
        loan.getDueDate(),
        loan.getDestinationNameSnapshot(),
        status.getLabel()
    );
  }
}
