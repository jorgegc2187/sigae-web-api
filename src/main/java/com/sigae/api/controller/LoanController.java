package com.sigae.api.controller;

import com.sigae.api.model.dto.CreateLoanPayload;
import com.sigae.api.model.dto.LoanAttachmentFile;
import com.sigae.api.model.dto.LoanDetailResponse;
import com.sigae.api.model.dto.LoanSummaryResponse;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/loans")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO', 'SOLO_LECTURA')")
public class LoanController {

  private final LoanService loanService;

  public LoanController(LoanService loanService) {
    this.loanService = loanService;
  }

  @GetMapping
  public List<LoanSummaryResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status
  ) {
    return loanService.findAll(search, status);
  }

  @GetMapping("/{loanId}")
  public LoanDetailResponse getById(@PathVariable UUID loanId) {
    return loanService.getDetail(loanId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public LoanDetailResponse create(
      @Valid @RequestPart("payload") CreateLoanPayload payload,
      @RequestPart(value = "signature", required = false) MultipartFile signature,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return loanService.create(payload, signature, attachments, authenticatedUser);
  }

  @PostMapping("/{loanId}/return")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO')")
  public LoanDetailResponse returnLoan(
      @PathVariable UUID loanId,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return loanService.returnLoan(loanId, authenticatedUser);
  }

  @GetMapping("/{loanId}/attachments/{attachmentId}")
  public ResponseEntity<byte[]> downloadAttachment(
      @PathVariable UUID loanId,
      @PathVariable UUID attachmentId
  ) {
    LoanAttachmentFile file = loanService.getAttachment(loanId, attachmentId);
    return ResponseEntity.ok()
        .contentType(file.contentType())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
        .body(file.content());
  }
}
