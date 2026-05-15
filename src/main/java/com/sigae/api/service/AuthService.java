package com.sigae.api.service;

import com.sigae.api.model.dto.AuthResponse;
import com.sigae.api.model.dto.AuthUserResponse;
import com.sigae.api.model.dto.ForgotPasswordRequest;
import com.sigae.api.model.dto.ResetPasswordRequest;
import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.entity.RefreshToken;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.PasswordResetRequestRepository;
import com.sigae.api.repository.RefreshTokenRepository;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.security.JwtService;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordResetRequestRepository passwordResetRequestRepository;
  private final OpaqueTokenService opaqueTokenService;
  private final TokenHashingService tokenHashingService;
  private final PasswordResetMailService passwordResetMailService;

  public AuthService(
      UserService userService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenRepository refreshTokenRepository,
      PasswordResetRequestRepository passwordResetRequestRepository,
      OpaqueTokenService opaqueTokenService,
      TokenHashingService tokenHashingService,
      PasswordResetMailService passwordResetMailService
  ) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordResetRequestRepository = passwordResetRequestRepository;
    this.opaqueTokenService = opaqueTokenService;
    this.tokenHashingService = tokenHashingService;
    this.passwordResetMailService = passwordResetMailService;
  }

  @Transactional
  public AuthResponse login(String email, String rawPassword) {
    User user;
    try {
      user = userService.getByEmailOrThrow(email);
    } catch (NotFoundException exception) {
      throw new BadCredentialsException("Credenciales inválidas.");
    }
    validateLogin(user, rawPassword);
    userService.markLoginSuccess(user);
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse refresh(String rawRefreshToken) {
    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHashingService.sha256(rawRefreshToken))
        .orElseThrow(() -> new BadCredentialsException("Refresh token inválido."));

    if (!refreshToken.isActive()) {
      throw new BadCredentialsException("Refresh token inválido o expirado.");
    }

    if (refreshToken.getUser().getStatus() != UserStatus.ACTIVE) {
      throw new BadCredentialsException("La cuenta se encuentra inactiva.");
    }

    refreshToken.revoke();
    refreshTokenRepository.save(refreshToken);
    return issueTokens(refreshToken.getUser());
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokenRepository.findByTokenHash(tokenHashingService.sha256(rawRefreshToken))
        .ifPresent(token -> {
          token.revoke();
          refreshTokenRepository.save(token);
        });
  }

  public AuthUserResponse me(AuthenticatedUser authenticatedUser) {
    return AuthUserResponse.from(userService.getById(authenticatedUser.userId()));
  }

  @Transactional
  public void requestPasswordReset(ForgotPasswordRequest request) {
    userService.findByEmail(request.email()).ifPresent(user -> {
      String rawToken = opaqueTokenService.generate();
      invalidateActivePasswordResetRequests(user.getId());
      PasswordResetRequest resetRequest = new PasswordResetRequest(
          user,
          tokenHashingService.sha256(rawToken),
          Instant.now().plus(jwtService.properties().passwordResetTokenTtl())
      );
      passwordResetRequestRepository.save(resetRequest);
      passwordResetMailService.sendPasswordResetMail(user, rawToken);
    });
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new BadRequestException("La confirmación de contraseña no coincide.");
    }

    validatePasswordPolicy(request.newPassword());

    PasswordResetRequest resetRequest = passwordResetRequestRepository
        .findByTokenHash(tokenHashingService.sha256(request.token()))
        .orElseThrow(() -> new BadRequestException("El enlace de recuperación es inválido o ya expiró."));

    if (!resetRequest.isActive()) {
      throw new BadRequestException("El enlace de recuperación es inválido o ya expiró.");
    }

    User user = resetRequest.getUser();
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    resetRequest.markUsed();
    revokeAllRefreshTokens(user.getId());
  }

  private void validateLogin(User user, String rawPassword) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new BadCredentialsException("La cuenta se encuentra inactiva.");
    }

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new BadCredentialsException("Credenciales inválidas.");
    }
  }

  private AuthResponse issueTokens(User user) {
    String rawRefreshToken = opaqueTokenService.generate();
    RefreshToken refreshToken = new RefreshToken(
        user,
        tokenHashingService.sha256(rawRefreshToken),
        Instant.now().plus(jwtService.properties().refreshTokenTtl())
    );
    refreshTokenRepository.save(refreshToken);

    return new AuthResponse(
        jwtService.createAccessToken(user),
        rawRefreshToken,
        "Bearer",
        jwtService.getAccessTokenExpiresInSeconds(),
        AuthUserResponse.from(user)
    );
  }

  private void invalidateActivePasswordResetRequests(java.util.UUID userId) {
    List<PasswordResetRequest> requests = passwordResetRequestRepository.findAllByUser_IdAndUsedAtIsNull(userId);
    if (requests.isEmpty()) {
      return;
    }

    requests.forEach(PasswordResetRequest::markUsed);
    passwordResetRequestRepository.saveAll(requests);
  }

  private void revokeAllRefreshTokens(java.util.UUID userId) {
    List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUser_Id(userId);
    if (refreshTokens.isEmpty()) {
      return;
    }

    refreshTokens.forEach(RefreshToken::revoke);
    refreshTokenRepository.saveAll(refreshTokens);
  }

  private void validatePasswordPolicy(String rawPassword) {
    boolean hasMinLength = rawPassword.length() >= 8;
    boolean hasUppercase = rawPassword.chars().anyMatch(Character::isUpperCase);
    boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
    boolean hasSpecial = rawPassword.chars().anyMatch(character ->
        !Character.isLetterOrDigit(character) && !Character.isWhitespace(character));

    if (!hasMinLength || !hasUppercase || !hasDigit || !hasSpecial) {
      throw new BadRequestException(
          "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial."
      );
    }
  }
}
