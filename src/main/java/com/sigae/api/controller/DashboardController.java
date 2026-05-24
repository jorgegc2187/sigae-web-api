package com.sigae.api.controller;

import com.sigae.api.model.dto.DashboardOverviewResponse;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO', 'SOLO_LECTURA')")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/overview")
  public DashboardOverviewResponse overview(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return dashboardService.overview(authenticatedUser);
  }
}
