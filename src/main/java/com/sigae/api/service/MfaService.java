package com.sigae.api.service;

import com.sigae.api.config.SecurityProperties;
import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.dto.MfaChallengeResponse;
import com.sigae.api.model.dto.MfaEnrollStartResponse;
import com.sigae.api.model.dto.UserMfaStatusResponse;
import com.sigae.api.model.entity.MfaChallenge;
import com.sigae.api.model.entity.MfaChallengePurpose;
import com.sigae.api.model.entity.RefreshToken;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserMfaSettings;
import com.sigae.api.repository.MfaChallengeRepository;
import com.sigae.api.repository.RefreshTokenRepository;
import com.sigae.api.repository.UserMfaSettingsRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MfaService {

  private static final String MFA_ERROR_MESSAGE = "El código de verificación no es válido o expiró.";

  private final UserService userService;
  private final UserMfaSettingsRepository settingsRepository;
  private final MfaChallengeRepository challengeRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OpaqueTokenService opaqueTokenService;
  private final TokenHashingService tokenHashingService;
  private final MfaSecretCipher secretCipher;
  private final TotpService totpService;
  private final SecurityProperties securityProperties;
  private final Clock clock;
  private final LiveNotificationPublisher liveNotificationPublisher;

  public MfaService(
      UserService userService,
      UserMfaSettingsRepository settingsRepository,
      MfaChallengeRepository challengeRepository,
      RefreshTokenRepository refreshTokenRepository,
      OpaqueTokenService opaqueTokenService,
      TokenHashingService tokenHashingService,
      MfaSecretCipher secretCipher,
      TotpService totpService,
      SecurityProperties securityProperties,
      Clock clock,
      LiveNotificationPublisher liveNotificationPublisher
  ) {
    this.userService = userService;
    this.settingsRepository = settingsRepository;
    this.challengeRepository = challengeRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.opaqueTokenService = opaqueTokenService;
    this.tokenHashingService = tokenHashingService;
    this.secretCipher = secretCipher;
    this.totpService = totpService;
    this.securityProperties = securityProperties;
    this.clock = clock;
    this.liveNotificationPublisher = liveNotificationPublisher;
  }

  public UserMfaStatusResponse getStatus(User user) {
    return settingsRepository.findByUser_Id(user.getId())
        .map(this::toStatus)
        .orElseGet(UserMfaStatusResponse::disabled);
  }

  public boolean requiresEnrollment(User user) {
    UserMfaSettings settings = settingsRepository.findByUser_Id(user.getId()).orElse(null);
    return settings != null && settings.isMfaRequired() && !settings.isMfaEnabled();
  }

  public boolean requiresChallenge(User user) {
    UserMfaSettings settings = settingsRepository.findByUser_Id(user.getId()).orElse(null);
    return settings != null && settings.isMfaRequired() && settings.isMfaEnabled();
  }

  @Transactional
  public MfaChallengeResponse createLoginChallenge(User user, MfaChallengePurpose purpose, String responseType) {
    String rawToken = opaqueTokenService.generate();
    MfaChallenge challenge = new MfaChallenge(
        user,
        tokenHashingService.sha256(rawToken),
        purpose,
        expiresAt()
    );
    challengeRepository.save(challenge);
    return new MfaChallengeResponse(responseType, rawToken, challengeExpiresInSeconds());
  }

  @Transactional
  public MfaEnrollStartResponse startEnrollment(String challengeToken) {
    MfaChallenge challenge = requireChallenge(challengeToken, MfaChallengePurpose.ENROLL);
    String secret = totpService.generateSecret();
    challenge.setEncryptedTotpSecret(secretCipher.encrypt(secret));
    challengeRepository.save(challenge);

    return new MfaEnrollStartResponse(
        totpService.createOtpAuthUri(securityProperties.mfa().issuer(), challenge.getUser().getEmail(), secret),
        secret,
        secondsUntil(challenge.getExpiresAt())
    );
  }

  @Transactional
  public User confirmEnrollment(String challengeToken, String code) {
    MfaChallenge challenge = requireChallenge(challengeToken, MfaChallengePurpose.ENROLL);
    if (challenge.getEncryptedTotpSecret() == null) {
      throw new BadRequestException("Debe iniciar el enrolamiento 2FA antes de confirmar el código.");
    }

    String secret = secretCipher.decrypt(challenge.getEncryptedTotpSecret());
    verifyCodeOrRecordFailure(challenge, secret, code);

    UserMfaSettings settings = requireSettings(challenge.getUser());
    settings.enable(secretCipher.encrypt(secret));
    settingsRepository.save(settings);
    challenge.markConsumed();
    challengeRepository.save(challenge);
    invalidateOpenChallenges(challenge.getUser().getId());
    liveNotificationPublisher.publishAdminInvalidation();
    return challenge.getUser();
  }

  @Transactional
  public User verifyLogin(String challengeToken, String code) {
    MfaChallenge challenge = requireChallenge(challengeToken, MfaChallengePurpose.LOGIN);
    UserMfaSettings settings = requireSettings(challenge.getUser());
    if (!settings.isMfaEnabled() || settings.getTotpSecretEncrypted() == null) {
      throw new BadRequestException(MFA_ERROR_MESSAGE);
    }

    String secret = secretCipher.decrypt(settings.getTotpSecretEncrypted());
    verifyCodeOrRecordFailure(challenge, secret, code);
    challenge.markConsumed();
    challengeRepository.save(challenge);
    return challenge.getUser();
  }

  @Transactional
  public UserMfaStatusResponse updatePolicy(UUID userId, boolean mfaRequired) {
    User user = userService.getById(userId);
    UserMfaSettings settings = getOrCreateSettings(user);
    if (mfaRequired) {
      settings.setMfaRequired(true);
    } else {
      settings.reset();
      invalidateOpenChallenges(userId);
      revokeRefreshTokens(userId);
    }
    settingsRepository.save(settings);
    liveNotificationPublisher.publishAdminInvalidation();
    return toStatus(settings);
  }

  @Transactional
  public UserMfaStatusResponse reset(UUID userId) {
    User user = userService.getById(userId);
    UserMfaSettings settings = getOrCreateSettings(user);
    settings.resetEnrollmentOnly();
    settingsRepository.save(settings);
    invalidateOpenChallenges(userId);
    revokeRefreshTokens(userId);
    liveNotificationPublisher.publishAdminInvalidation();
    return toStatus(settings);
  }

  private UserMfaSettings getOrCreateSettings(User user) {
    return settingsRepository.findByUser_Id(user.getId()).orElseGet(() -> settingsRepository.save(new UserMfaSettings(user)));
  }

  private UserMfaSettings requireSettings(User user) {
    return settingsRepository.findByUser_Id(user.getId())
        .orElseThrow(() -> new BadRequestException(MFA_ERROR_MESSAGE));
  }

  private MfaChallenge requireChallenge(String challengeToken, MfaChallengePurpose purpose) {
    MfaChallenge challenge = challengeRepository.findByTokenHash(tokenHashingService.sha256(challengeToken))
        .orElseThrow(() -> new BadRequestException(MFA_ERROR_MESSAGE));

    if (challenge.getPurpose() != purpose || !challenge.isActive()) {
      throw new BadRequestException(MFA_ERROR_MESSAGE);
    }

    return challenge;
  }

  private void verifyCodeOrRecordFailure(MfaChallenge challenge, String secret, String code) {
    if (totpService.verify(secret, code)) {
      return;
    }

    challenge.recordFailure();
    if (challenge.getFailedAttempts() >= securityProperties.mfa().maxAttempts()) {
      challenge.markConsumed();
    }
    challengeRepository.save(challenge);
    throw new BadRequestException(MFA_ERROR_MESSAGE);
  }

  private void invalidateOpenChallenges(UUID userId) {
    List<MfaChallenge> openChallenges = challengeRepository.findAllByUser_IdAndConsumedAtIsNull(userId);
    if (openChallenges.isEmpty()) {
      return;
    }
    openChallenges.forEach(MfaChallenge::markConsumed);
    challengeRepository.saveAll(openChallenges);
  }

  private void revokeRefreshTokens(UUID userId) {
    List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUser_Id(userId);
    if (refreshTokens.isEmpty()) {
      return;
    }
    refreshTokens.forEach(RefreshToken::revoke);
    refreshTokenRepository.saveAll(refreshTokens);
  }

  private UserMfaStatusResponse toStatus(UserMfaSettings settings) {
    return new UserMfaStatusResponse(
        settings.isMfaRequired(),
        settings.isMfaEnabled(),
        settings.getMfaEnabledAt()
    );
  }

  private Instant expiresAt() {
    return clock.instant().plus(securityProperties.mfa().challengeTtl());
  }

  private long challengeExpiresInSeconds() {
    return securityProperties.mfa().challengeTtl().toSeconds();
  }

  private long secondsUntil(Instant expiresAt) {
    Duration duration = Duration.between(clock.instant(), expiresAt);
    return Math.max(0, duration.toSeconds());
  }
}
