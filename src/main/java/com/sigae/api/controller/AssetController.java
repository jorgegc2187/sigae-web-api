package com.sigae.api.controller;

import com.sigae.api.model.dto.AssetRequest;
import com.sigae.api.model.dto.AssetInventoryGroupResponse;
import com.sigae.api.model.dto.AssetResponse;
import com.sigae.api.model.dto.AssetAttachmentFile;
import com.sigae.api.model.dto.AssetTraceabilityResponse;
import com.sigae.api.model.dto.AssetStatusChangeRequest;
import com.sigae.api.model.dto.AssetTraceabilityAttachmentFile;
import com.sigae.api.service.AssetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.sigae.api.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

  private final AssetService assetService;

  public AssetController(AssetService assetService) {
    this.assetService = assetService;
  }

  @GetMapping
  public List<AssetResponse> list() {
    return assetService.findAllResponses();
  }

  @GetMapping("/grouped")
  public List<AssetInventoryGroupResponse> listGrouped(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID categoryId
  ) {
    return assetService.findGrouped(search, categoryId);
  }

  @GetMapping("/grouped/{groupId}")
  public AssetInventoryGroupResponse getGroupedById(@PathVariable String groupId) {
    return assetService.findGroupedById(groupId);
  }

  @GetMapping("/lookup")
  public AssetResponse lookup(@RequestParam String value) {
    return assetService.lookupResponseByScanValue(value);
  }

  @GetMapping("/{assetId}")
  public AssetResponse getById(@PathVariable UUID assetId) {
    return assetService.getResponseById(assetId);
  }

  @GetMapping("/{assetId}/traceability")
  public List<AssetTraceabilityResponse> traceability(@PathVariable UUID assetId) {
    return assetService.getTraceability(assetId).stream().map(AssetTraceabilityResponse::from).toList();
  }

  @GetMapping("/{assetId}/attachments/{attachmentId}")
  public ResponseEntity<byte[]> downloadAttachment(
      @PathVariable UUID assetId,
      @PathVariable UUID attachmentId
  ) {
    AssetAttachmentFile file = assetService.getAttachment(assetId, attachmentId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
        .contentType(file.mediaType())
        .body(file.content());
  }

  @GetMapping("/{assetId}/traceability/{traceabilityId}/attachments/{attachmentId}")
  public ResponseEntity<byte[]> downloadTraceabilityAttachment(
      @PathVariable UUID assetId,
      @PathVariable UUID traceabilityId,
      @PathVariable UUID attachmentId
  ) {
    AssetTraceabilityAttachmentFile file = assetService.getTraceabilityAttachment(assetId, traceabilityId, attachmentId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
        .contentType(file.mediaType())
        .body(file.content());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public AssetResponse create(
      @Valid @RequestPart("payload") AssetRequest request,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return assetService.getResponseById(assetService.create(request, attachments, authenticatedUser).getId());
  }

  @PatchMapping("/{assetId}")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public AssetResponse update(
      @PathVariable UUID assetId,
      @Valid @RequestPart("payload") AssetRequest request,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return assetService.getResponseById(assetService.update(assetId, request, attachments, authenticatedUser).getId());
  }

  @PostMapping("/{assetId}/status-change")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public AssetResponse changeStatus(
      @PathVariable UUID assetId,
      @Valid @RequestPart("payload") AssetStatusChangeRequest request,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return assetService.getResponseById(
        assetService.changeStatus(assetId, request, attachments, authenticatedUser).getId()
    );
  }
}
