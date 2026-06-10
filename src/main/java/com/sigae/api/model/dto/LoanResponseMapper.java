package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.LoanAsset;

final class LoanResponseMapper {
  private LoanResponseMapper() {}

  static LoanTeacherResponse teacherFrom(Loan loan) {
    return new LoanTeacherResponse(
        loan.getTeacherNameSnapshot(),
        buildInitials(loan.getTeacherNameSnapshot()),
        loan.getTeacherSpecialtySnapshot(),
        loan.getTeacherDniSnapshot()
    );
  }

  static LoanAssetResponse assetFrom(LoanAsset loanAsset) {
    return new LoanAssetResponse(
        loanAsset.getAsset().getId(),
        loanAsset.getAssetCodeSnapshot(),
        loanAsset.getAssetNameSnapshot(),
        loanAsset.getAssetCategorySnapshot(),
        loanAsset.getLoan().getCompletedAt() == null ? "En préstamo" : conditionLabel(loanAsset.getAsset().getCondition())
    );
  }

  private static String conditionLabel(AssetCondition condition) {
    if (condition == null) {
      return "Operativo";
    }

    return switch (condition) {
      case BUENO -> "Operativo";
      case REGULAR -> "Regular";
      case MALO -> "Malo";
      case MANTENIMIENTO -> "Mantenimiento";
      case DADO_DE_BAJA -> "Dado de baja";
    };
  }

  private static String buildInitials(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return "";
    }

    String[] parts = fullName.trim().split("\\s+");
    StringBuilder initials = new StringBuilder();
    for (int index = 0; index < Math.min(2, parts.length); index++) {
      initials.append(parts[index].charAt(0));
    }
    return initials.toString().toUpperCase();
  }
}
