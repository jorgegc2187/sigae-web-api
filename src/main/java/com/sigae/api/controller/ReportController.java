package com.sigae.api.controller;

import com.sigae.api.model.dto.AssetReportRowResponse;
import com.sigae.api.model.dto.LoanReportRowResponse;
import com.sigae.api.model.dto.PhysicalInventoryReportResponse;
import com.sigae.api.model.dto.ReportExportFile;
import com.sigae.api.model.dto.ReportExportFormat;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO', 'SOLO_LECTURA')")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/assets")
  public List<AssetReportRowResponse> listAssets(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate
  ) {
    return reportService.listAssetRows(categoryId, locationId, startDate, endDate);
  }

  @GetMapping("/assets/physical-inventory")
  public PhysicalInventoryReportResponse physicalInventory(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return reportService.physicalInventory(categoryId, locationId, startDate, endDate, authenticatedUser);
  }

  @GetMapping("/assets/export")
  public ResponseEntity<byte[]> exportAssets(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "pdf") String format,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    ReportExportFile file = reportService.exportAssetRows(
        categoryId,
        locationId,
        startDate,
        endDate,
        ReportExportFormat.from(format),
        authenticatedUser
    );

    return ResponseEntity.ok()
        .contentType(file.contentType())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
        .body(file.content());
  }

  @GetMapping("/loans")
  public List<LoanReportRowResponse> listLoans(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate
  ) {
    return reportService.listLoanRows(search, locationId, startDate, endDate);
  }

  @GetMapping("/loans/export")
  public ResponseEntity<byte[]> exportLoans(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "pdf") String format,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    ReportExportFile file = reportService.exportLoanRows(
        search,
        locationId,
        startDate,
        endDate,
        ReportExportFormat.from(format),
        authenticatedUser
    );

    return ResponseEntity.ok()
        .contentType(file.contentType())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
        .body(file.content());
  }
}
