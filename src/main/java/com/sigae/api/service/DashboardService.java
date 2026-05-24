package com.sigae.api.service;

import com.sigae.api.model.dto.DashboardOverviewResponse;
import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.Loan;
import com.sigae.api.model.entity.LoanAsset;
import com.sigae.api.model.entity.TraceabilityEventType;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.AssetTraceabilityRepository;
import com.sigae.api.repository.LoanRepository;
import com.sigae.api.security.AuthenticatedUser;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

  private static final UUID GLOBAL_SCOPE_PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final int ALERT_WINDOW_DAYS = 3;
  private static final int MAX_ALERTS = 5;
  private static final int MAX_MOVEMENTS = 5;
  private static final int MAX_TOP_CATEGORIES = 3;

  private final AssetRepository assetRepository;
  private final LoanRepository loanRepository;
  private final AssetTraceabilityRepository traceabilityRepository;

  public DashboardService(
      AssetRepository assetRepository,
      LoanRepository loanRepository,
      AssetTraceabilityRepository traceabilityRepository
  ) {
    this.assetRepository = assetRepository;
    this.loanRepository = loanRepository;
    this.traceabilityRepository = traceabilityRepository;
  }

  public DashboardOverviewResponse overview(AuthenticatedUser authenticatedUser) {
    DashboardScope scope = resolveScope(authenticatedUser);
    LocalDate today = LocalDate.now();

    long totalAssets = assetRepository.countForDashboard(scope.applyScope(), scope.locationIds());
    Map<AssetCondition, Long> conditionCounts = loadConditionCounts(scope);

    long operationalAssets = conditionCounts.getOrDefault(AssetCondition.BUENO, 0L)
        + conditionCounts.getOrDefault(AssetCondition.REGULAR, 0L);
    long maintenanceAssets = conditionCounts.getOrDefault(AssetCondition.MANTENIMIENTO, 0L);
    long decommissionedAssets = conditionCounts.getOrDefault(AssetCondition.DADO_DE_BAJA, 0L);

    DashboardOverviewResponse.DashboardMetrics metrics = new DashboardOverviewResponse.DashboardMetrics(
        totalAssets,
        operationalAssets,
        maintenanceAssets,
        decommissionedAssets,
        loanRepository.countActiveForDashboard(scope.applyScope(), scope.locationIds()),
        loanRepository.countOverdueForDashboard(today, scope.applyScope(), scope.locationIds()),
        loanRepository.countDueTodayForDashboard(today, scope.applyScope(), scope.locationIds()),
        totalAssets == 0 ? 0D : roundToSingleDecimal((operationalAssets * 100.0D) / totalAssets)
    );

    DashboardOverviewResponse.DashboardConditionBreakdown conditionBreakdown =
        new DashboardOverviewResponse.DashboardConditionBreakdown(
            conditionCounts.getOrDefault(AssetCondition.BUENO, 0L),
            conditionCounts.getOrDefault(AssetCondition.REGULAR, 0L),
            conditionCounts.getOrDefault(AssetCondition.MALO, 0L),
            maintenanceAssets,
            decommissionedAssets
        );

    List<DashboardOverviewResponse.DashboardCategoryShare> topCategories =
        assetRepository.countByCategoryForDashboard(scope.applyScope(), scope.locationIds()).stream()
            .limit(MAX_TOP_CATEGORIES)
            .map(category -> new DashboardOverviewResponse.DashboardCategoryShare(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getTotal(),
                totalAssets == 0 ? 0D : roundToSingleDecimal((category.getTotal() * 100.0D) / totalAssets)
            ))
            .toList();

    List<DashboardOverviewResponse.DashboardLoanAlert> loanAlerts =
        loanRepository.findDashboardAlerts(today.plusDays(ALERT_WINDOW_DAYS), scope.applyScope(), scope.locationIds()).stream()
            .filter(loan -> loan.getDueDate().isBefore(today) || !loan.getDueDate().isAfter(today.plusDays(ALERT_WINDOW_DAYS)))
            .limit(MAX_ALERTS)
            .map(loan -> toLoanAlert(loan, today))
            .toList();

    List<DashboardOverviewResponse.DashboardRecentMovement> recentMovements =
        traceabilityRepository.findRecentForDashboard(scope.applyScope(), scope.locationIds(), PageRequest.of(0, MAX_MOVEMENTS)).stream()
            .map(this::toRecentMovement)
            .toList();

    return new DashboardOverviewResponse(metrics, conditionBreakdown, topCategories, loanAlerts, recentMovements);
  }

  private DashboardOverviewResponse.DashboardLoanAlert toLoanAlert(Loan loan, LocalDate today) {
    return new DashboardOverviewResponse.DashboardLoanAlert(
        loan.getId(),
        loan.getCode(),
        loan.getTeacherNameSnapshot(),
        firstAssetName(loan),
        loan.getDestinationNameSnapshot(),
        buildDueStatusLabel(loan.getDueDate(), today),
        buildAlertSeverity(loan.getDueDate(), today),
        loan.getDueDate().toString()
    );
  }

  private DashboardOverviewResponse.DashboardRecentMovement toRecentMovement(AssetTraceability traceability) {
    return new DashboardOverviewResponse.DashboardRecentMovement(
        traceability.getAsset().getId(),
        traceability.getAsset().getCode(),
        traceability.getAsset().getName(),
        traceability.getAsset().getAssetType().getCategory().getName(),
        traceability.getAsset().getCondition().getLabel(),
        movementLabel(traceability.getEventType()),
        traceability.getOccurredAt()
    );
  }

  private Map<AssetCondition, Long> loadConditionCounts(DashboardScope scope) {
    Map<AssetCondition, Long> counts = new EnumMap<>(AssetCondition.class);
    assetRepository.countByConditionForDashboard(scope.applyScope(), scope.locationIds())
        .forEach(entry -> counts.put(entry.getCondition(), entry.getTotal()));
    return counts;
  }

  private DashboardScope resolveScope(AuthenticatedUser authenticatedUser) {
    if (authenticatedUser == null || authenticatedUser.role() != UserRole.ENCARGADO) {
      return new DashboardScope(false, List.of(GLOBAL_SCOPE_PLACEHOLDER));
    }

    List<UUID> locationIds = authenticatedUser.locationIds().stream()
        .map(UUID::fromString)
        .toList();

    return locationIds.isEmpty()
        ? new DashboardScope(false, List.of(GLOBAL_SCOPE_PLACEHOLDER))
        : new DashboardScope(true, locationIds);
  }

  private String firstAssetName(Loan loan) {
    return loan.getAssets().stream()
        .findFirst()
        .map(LoanAsset::getAssetNameSnapshot)
        .orElse("Sin activo asociado");
  }

  private String buildDueStatusLabel(LocalDate dueDate, LocalDate today) {
    long days = ChronoUnit.DAYS.between(today, dueDate);
    if (days < 0) {
      long overdueDays = Math.abs(days);
      return overdueDays == 1 ? "Venció hace 1 día" : "Venció hace %d días".formatted(overdueDays);
    }

    if (days == 0) {
      return "Vence hoy";
    }

    return days == 1 ? "Vence en 1 día" : "Vence en %d días".formatted(days);
  }

  private String buildAlertSeverity(LocalDate dueDate, LocalDate today) {
    if (dueDate.isBefore(today)) {
      return "overdue";
    }

    return dueDate.isEqual(today) ? "due_today" : "due_soon";
  }

  private String movementLabel(TraceabilityEventType eventType) {
    return switch (eventType) {
      case CREATED -> "Alta";
      case UPDATED -> "Actualización";
      case CONDITION_CHANGED -> "Cambio de estado";
      case LOCATION_CHANGED -> "Cambio de ubicación";
      case DECOMMISSIONED -> "Baja";
      case LOANED -> "Préstamo";
      case RETURNED -> "Devolución";
    };
  }

  private double roundToSingleDecimal(double value) {
    return Math.round(value * 10.0D) / 10.0D;
  }

  private record DashboardScope(boolean applyScope, List<UUID> locationIds) {}
}
