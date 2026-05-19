package com.sigae.api.controller;

import com.sigae.api.model.dto.InstitutionBrandingResponse;
import com.sigae.api.model.dto.InstitutionLogoFile;
import com.sigae.api.model.dto.InstitutionSettingsResponse;
import com.sigae.api.model.dto.UpdateInstitutionSettingsRequest;
import com.sigae.api.service.InstitutionSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/settings")
public class InstitutionSettingsController {

  private final InstitutionSettingsService institutionSettingsService;

  public InstitutionSettingsController(InstitutionSettingsService institutionSettingsService) {
    this.institutionSettingsService = institutionSettingsService;
  }

  @GetMapping("/branding")
  public InstitutionBrandingResponse getBranding() {
    return InstitutionBrandingResponse.from(institutionSettingsService.getCurrentSettings());
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public InstitutionSettingsResponse getSettings() {
    return InstitutionSettingsResponse.from(institutionSettingsService.getCurrentSettings());
  }

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public InstitutionSettingsResponse update(
      @Valid @RequestPart("payload") UpdateInstitutionSettingsRequest request,
      @RequestPart(value = "logo", required = false) MultipartFile logo
  ) {
    return InstitutionSettingsResponse.from(institutionSettingsService.update(request, logo));
  }

  @GetMapping("/logo")
  public ResponseEntity<byte[]> getLogo() {
    return institutionSettingsService.getLogoFile()
        .map(this::toFileResponse)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ResponseEntity<byte[]> toFileResponse(InstitutionLogoFile file) {
    return ResponseEntity.ok()
        .contentType(file.contentType())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(file.filename()).build().toString()
        )
        .body(file.content());
  }
}
