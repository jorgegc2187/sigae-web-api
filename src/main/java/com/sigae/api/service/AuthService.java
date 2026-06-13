package com.sigae.api.service;

import com.sigae.api.model.dto.AuthResponse;
import com.sigae.api.model.dto.AuthUserResponse;
import com.sigae.api.model.dto.ForgotPasswordRequest;
import com.sigae.api.model.dto.MfaChallengeResponse;
import com.sigae.api.model.dto.MfaEnrollConfirmRequest;
import com.sigae.api.model.dto.MfaEnrollStartRequest;
import com.sigae.api.model.dto.MfaEnrollStartResponse;
import com.sigae.api.model.dto.MfaVerifyRequest;
import com.sigae.api.model.dto.ResetPasswordRequest;
import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.entity.MfaChallengePurpose;
import com.sigae.api.model.entity.PasswordResetPurpose;
import com.sigae.api.model.entity.PasswordResetRequest;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.entity.RefreshToken;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.RefreshTokenRepository;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
  private final OpaqueTokenService opaqueTokenService;
  private final PasswordSetupTokenService passwordSetupTokenService;
  private final PasswordResetMailService passwordResetMailService;
  private final TokenHashingService tokenHashingService;
  private final MfaService mfaService;
  private final LiveNotificationPublisher liveNotificationPublisher;
  private final AuthAbuseProtectionService authAbuseProtectionService;

  public AuthService(
      UserService userService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenRepository refreshTokenRepository,
      OpaqueTokenService opaqueTokenService,
      PasswordSetupTokenService passwordSetupTokenService,
      PasswordResetMailService passwordResetMailService,
      TokenHashingService tokenHashingService,
      MfaService mfaService,
      LiveNotificationPublisher liveNotificationPublisher,
      AuthAbuseProtectionService authAbuseProtectionService
  ) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.opaqueTokenService = opaqueTokenService;
    this.passwordSetupTokenService = passwordSetupTokenService;
    this.passwordResetMailService = passwordResetMailService;
    this.tokenHashingService = tokenHashingService;
    this.mfaService = mfaService;
    this.liveNotificationPublisher = liveNotificationPublisher;
    this.authAbuseProtectionService = authAbuseProtectionService;
  }

  @Transactional
  public Object login(String email, String rawPassword, HttpServletRequest request) {
    authAbuseProtectionService.checkLoginIpAllowed(request);
    String normalizedEmail = normalizeEmail(email);
    User user;
    try {
      user = userService.getByEmailOrThrow(normalizedEmail);
    } catch (NotFoundException exception) {
      throw new BadCredentialsException("Credenciales inválidas.");
    }
    authAbuseProtectionService.ensureAccountNotLocked(user);
    validateLogin(user, rawPassword);
    userService.clearFailedLoginState(user);
    if (mfaService.requiresEnrollment(user)) {
      return mfaService.createLoginChallenge(user, MfaChallengePurpose.ENROLL, "MFA_ENROLL_REQUIRED");
    }
    if (mfaService.requiresChallenge(user)) {
      return mfaService.createLoginChallenge(user, MfaChallengePurpose.LOGIN, "MFA_CHALLENGE_REQUIRED");
    }
    userService.markLoginSuccess(user);
    return issueTokens(user);
  }

  @Transactional
  public MfaEnrollStartResponse startMfaEnrollment(MfaEnrollStartRequest request, HttpServletRequest servletRequest) {
    authAbuseProtectionService.checkMfaStartAllowed(request.challengeToken(), servletRequest);
    return mfaService.startEnrollment(request.challengeToken());
  }

  @Transactional
  public AuthResponse confirmMfaEnrollment(MfaEnrollConfirmRequest request, HttpServletRequest servletRequest) {
    authAbuseProtectionService.checkMfaVerifyAllowed(request.challengeToken(), servletRequest);
    User user = mfaService.confirmEnrollment(request.challengeToken(), request.code());
    userService.markLoginSuccess(user);
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse verifyMfa(MfaVerifyRequest request, HttpServletRequest servletRequest) {
    authAbuseProtectionService.checkMfaVerifyAllowed(request.challengeToken(), servletRequest);
    User user = mfaService.verifyLogin(request.challengeToken(), request.code());
    userService.markLoginSuccess(user);
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse refresh(String rawRefreshToken, HttpServletRequest request) {
    authAbuseProtectionService.checkRefreshAllowed(rawRefreshToken, request);
    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHashingService.sha256(rawRefreshToken))
        .orElseThrow(() -> new BadCredentialsException("Refresh token inválido."));

    if (!refreshToken.isActive()) {
      throw new BadCredentialsException("Refresh token inválido o expirado.");
    }

    validateUserIsActive(refreshToken.getUser());

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
    User user = userService.getById(authenticatedUser.userId());
    return AuthUserResponse.from(user, mfaService.getStatus(user));
  }

  @Transactional
  public void requestPasswordReset(ForgotPasswordRequest request, HttpServletRequest servletRequest) {
    authAbuseProtectionService.checkForgotPasswordAllowed(request.email(), servletRequest);
    userService.findByEmail(request.email()).ifPresent(user -> {
      String rawToken = passwordSetupTokenService.issuePasswordResetToken(user);
      passwordResetMailService.sendPasswordResetMail(user, rawToken);
    });
  }

  public void validateResetPasswordToken(String token, HttpServletRequest request) {
    authAbuseProtectionService.checkResetPasswordValidateAllowed(token, request);
    passwordSetupTokenService.validateToken(token);
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request, HttpServletRequest servletRequest) {
    authAbuseProtectionService.checkResetPasswordSubmitAllowed(request.token(), servletRequest);
    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new BadRequestException("La confirmación de contraseña no coincide.");
    }

    validatePasswordPolicy(request.newPassword());

    PasswordResetRequest resetRequest = passwordSetupTokenService.consumeValidToken(request.token());
    User user = resetRequest.getUser();
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    boolean activatedPendingAccount = false;
    if (resetRequest.getPurpose() == PasswordResetPurpose.ACCOUNT_SETUP && user.getStatus() == UserStatus.PENDING) {
      user.setStatus(UserStatus.ACTIVE);
      activatedPendingAccount = true;
    }
    resetRequest.markUsed();
    revokeAllRefreshTokens(user.getId());
    if (activatedPendingAccount) {
      liveNotificationPublisher.publishAdminInvalidation();
    }
  }

  private void validateLogin(User user, String rawPassword) {
    validateUserIsActive(user);

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      authAbuseProtectionService.registerFailedLogin(user);
      throw new BadCredentialsException("Credenciales inválidas.");
    }
  }

  private void validateUserIsActive(User user) {
    if (user.getStatus() == UserStatus.PENDING) {
      throw new BadCredentialsException("La cuenta aún no ha completado la activación.");
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new BadCredentialsException("La cuenta se encuentra inactiva.");
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
        AuthUserResponse.from(user, mfaService.getStatus(user))
    );
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

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
