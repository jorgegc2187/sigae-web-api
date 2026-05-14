package com.sigae.api.controller;

import com.sigae.api.model.dto.AssetRequest;
import com.sigae.api.model.dto.AssetResponse;
import com.sigae.api.model.dto.AssetTraceabilityResponse;
import com.sigae.api.service.AssetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

  private final AssetService assetService;

  public AssetController(AssetService assetService) {
    this.assetService = assetService;
  }

  @GetMapping
  public List<AssetResponse> list() {
    return assetService.findAll().stream().map(AssetResponse::from).toList();
  }

  @GetMapping("/{assetId}")
  public AssetResponse getById(@PathVariable UUID assetId) {
    return AssetResponse.from(assetService.getById(assetId));
  }

  @GetMapping("/{assetId}/traceability")
  public List<AssetTraceabilityResponse> traceability(@PathVariable UUID assetId) {
    return assetService.getTraceability(assetId).stream().map(AssetTraceabilityResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public AssetResponse create(@Valid @RequestBody AssetRequest request) {
    return AssetResponse.from(assetService.create(request));
  }

  @PatchMapping("/{assetId}")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public AssetResponse update(
      @PathVariable UUID assetId,
      @Valid @RequestBody AssetRequest request
  ) {
    return AssetResponse.from(assetService.update(assetId, request));
  }
}
