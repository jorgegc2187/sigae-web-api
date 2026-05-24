package com.sigae.api.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardOverviewResponse(
    DashboardMetrics metrics,
    DashboardConditionBreakdown conditionBreakdown,
    List<DashboardCategoryShare> topCategories,
    List<DashboardLoanAlert> loanAlerts,
    List<DashboardRecentMovement> recentMovements
) {
  public record DashboardMetrics(
      long totalAssets,
      long operationalAssets,
      long maintenanceAssets,
      long decommissionedAssets,
      long activeLoans,
      long overdueLoans,
      long dueTodayLoans,
      double healthPercentage
  ) {}

  public record DashboardConditionBreakdown(
      long good,
      long regular,
      long bad,
      long maintenance,
      long decommissioned
  ) {}

  public record DashboardCategoryShare(
      UUID categoryId,
      String categoryName,
      long totalAssets,
      double percentage
  ) {}

  public record DashboardLoanAlert(
      UUID loanId,
      String loanCode,
      String teacherName,
      String assetName,
      String locationName,
      String dueStatusLabel,
      String severity,
      String dueDate
  ) {}

  public record DashboardRecentMovement(
      UUID assetId,
      String assetCode,
      String assetName,
      String categoryName,
      String condition,
      String movementType,
      Instant occurredAt
  ) {}
}
