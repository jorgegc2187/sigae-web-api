package com.sigae.api.service;

import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.entity.PasswordResetRequest;
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
  public String issueToken(User user) {
    invalidateActiveRequests(user.getId());
    String rawToken = opaqueTokenService.generate();
    PasswordResetRequest resetRequest = new PasswordResetRequest(
        user,
        tokenHashingService.sha256(rawToken),
        Instant.now().plus(jwtService.properties().passwordResetTokenTtl())
    );
    passwordResetRequestRepository.save(resetRequest);
    return rawToken;
  }

  public PasswordResetRequest consumeValidToken(String rawToken) {
    PasswordResetRequest resetRequest = passwordResetRequestRepository
        .findByTokenHash(tokenHashingService.sha256(rawToken))
        .orElseThrow(() -> new BadRequestException("El enlace de recuperación es inválido o ya expiró."));

    if (!resetRequest.isActive()) {
      throw new BadRequestException("El enlace de recuperación es inválido o ya expiró.");
    }

    return resetRequest;
  }

  @Transactional
  public void invalidateActiveRequests(UUID userId) {
    List<PasswordResetRequest> requests = passwordResetRequestRepository.findAllByUser_IdAndUsedAtIsNull(userId);
    if (requests.isEmpty()) {
      return;
    }

    requests.forEach(PasswordResetRequest::markUsed);
    passwordResetRequestRepository.saveAll(requests);
  }
}
