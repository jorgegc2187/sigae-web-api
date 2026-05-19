package com.sigae.api.service;

import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.dto.UserInvitationInfo;
import com.sigae.api.model.entity.InvitationStatus;
import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.User;
import com.sigae.api.repository.PasswordResetRequestRepository;
import com.sigae.api.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PasswordSetupTokenService {

  private final PasswordResetRequestRepository passwordResetRequestRepository;
  private final OpaqueTokenService opaqueTokenService;
  private final TokenHashingService tokenHashingService;
  private final JwtService jwtService;

  public PasswordSetupTokenService(
      PasswordResetRequestRepository passwordResetRequestRepository,
      OpaqueTokenService opaqueTokenService,
      TokenHashingService tokenHashingService,
      JwtService jwtService
  ) {
    this.passwordResetRequestRepository = passwordResetRequestRepository;
    this.opaqueTokenService = opaqueTokenService;
    this.tokenHashingService = tokenHashingService;
    this.jwtService = jwtService;
  }

  @Transactional
  public String issueAccountSetupToken(User user) {
    cancelOpenRequests(user.getId(), PasswordResetPurpose.ACCOUNT_SETUP);
    String rawToken = opaqueTokenService.generate();
    PasswordResetRequest resetRequest = new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().plus(jwtService.properties().passwordResetTokenTtl()),
        PasswordResetPurpose.ACCOUNT_SETUP
    );
    passwordResetRequestRepository.save(resetRequest);
    return rawToken;
  }

  @Transactional
  public String issuePasswordResetToken(User user) {
    cancelOpenRequests(user.getId(), PasswordResetPurpose.PASSWORD_RESET);
    String rawToken = opaqueTokenService.generate();
    PasswordResetRequest resetRequest = new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().plus(jwtService.properties().passwordResetTokenTtl()),
        PasswordResetPurpose.PASSWORD_RESET
    );
    passwordResetRequestRepository.save(resetRequest);
    return rawToken;
  }

  public PasswordResetRequest consumeValidToken(String rawToken) {
    return requireActiveRequest(rawToken);
  }

  public void validateToken(String rawToken) {
    requireActiveRequest(rawToken);
  }

  @Transactional
  public void cancelInvitation(UUID userId) {
    PasswordResetRequest invitation = passwordResetRequestRepository
        .findTopByUser_IdAndPurposeOrderByCreatedAtDesc(userId, PasswordResetPurpose.ACCOUNT_SETUP)
        .orElseThrow(() -> new BadRequestException("El usuario no tiene una invitación activa para anular."));

    if (!invitation.isActive()) {
      throw new BadRequestException("El usuario no tiene una invitación activa para anular.");
    }

    invitation.markCancelled();
    passwordResetRequestRepository.save(invitation);
  }

  public UserInvitationInfo getInvitationInfo(UUID userId) {
    return passwordResetRequestRepository
        .findTopByUser_IdAndPurposeOrderByCreatedAtDesc(userId, PasswordResetPurpose.ACCOUNT_SETUP)
        .map(request -> new UserInvitationInfo(resolveInvitationStatus(request), request.getExpiresAt()))
        .orElse(null);
  }

  @Transactional
  public void cancelOpenRequests(UUID userId, PasswordResetPurpose purpose) {
    List<PasswordResetRequest> requests = passwordResetRequestRepository
        .findAllByUser_IdAndPurposeAndUsedAtIsNullAndCancelledAtIsNull(userId, purpose);
    if (requests.isEmpty()) {
      return;
    }

    requests.forEach(PasswordResetRequest::markCancelled);
    passwordResetRequestRepository.saveAll(requests);
  }

  private InvitationStatus resolveInvitationStatus(PasswordResetRequest request) {
    if (request.isCancelled()) {
      return InvitationStatus.CANCELLED;
    }

    if (request.isUsed()) {
      return InvitationStatus.CONSUMED;
    }

    if (request.isExpired()) {
      return InvitationStatus.EXPIRED;
    }

    return InvitationStatus.ACTIVE;
  }

  private PasswordResetRequest requireActiveRequest(String rawToken) {
    PasswordResetRequest resetRequest = passwordResetRequestRepository
        .findByTokenHash(tokenHashingService.sha256(rawToken))
        .orElseThrow(() -> new BadRequestException("El enlace de recuperación es inválido o ya expiró."));

    if (!resetRequest.isActive()) {
      throw new BadRequestException("El enlace de recuperación es inválido o ya expiró.");
    }

    return resetRequest;
  }
}
