package com.sigae.api.service;

import com.sigae.api.config.SecurityProperties;
import com.sigae.api.exception.RateLimitExceededException;
import com.sigae.api.model.entity.AuthRateLimitBucket;
import com.sigae.api.model.entity.User;
import com.sigae.api.repository.AuthRateLimitBucketRepository;
import com.sigae.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthAbuseProtectionService {

  private static final String AUTH_RATE_LIMITED = "AUTH_RATE_LIMITED";
  private static final String AUTH_ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";
  private static final String GLOBAL_SUBJECT = "__global__";
  private static final String RATE_LIMIT_MESSAGE = "Demasiados intentos. Intenta nuevamente en unos minutos.";
  private static final String ACCOUNT_LOCK_MESSAGE = "Demasiados intentos de acceso. Intenta nuevamente en unos minutos.";

  private final SecurityProperties securityProperties;
  private final AuthRateLimitBucketRepository bucketRepository;
  private final UserRepository userRepository;
  private final TokenHashingService tokenHashingService;
  private final ClientRequestFingerprintResolver clientRequestFingerprintResolver;
  private final Clock clock;

  public AuthAbuseProtectionService(
      SecurityProperties securityProperties,
      AuthRateLimitBucketRepository bucketRepository,
      UserRepository userRepository,
      TokenHashingService tokenHashingService,
      ClientRequestFingerprintResolver clientRequestFingerprintResolver,
      Clock clock
  ) {
    this.securityProperties = securityProperties;
    this.bucketRepository = bucketRepository;
    this.userRepository = userRepository;
    this.tokenHashingService = tokenHashingService;
    this.clientRequestFingerprintResolver = clientRequestFingerprintResolver;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkLoginIpAllowed(HttpServletRequest request) {
    SecurityProperties.Login login = securityProperties.abuseProtection().login();
    checkAndConsume(
        "AUTH_LOGIN_IP",
        GLOBAL_SUBJECT,
        resolveClientFingerprint(request),
        login.ipMaxAttempts(),
        login.ipWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  public void ensureAccountNotLocked(User user) {
    Instant lockedUntil = user.getLockedUntil();
    Instant now = now();
    if (lockedUntil != null && lockedUntil.isAfter(now)) {
      throw new RateLimitExceededException(
          ACCOUNT_LOCK_MESSAGE,
          AUTH_ACCOUNT_LOCKED,
          Duration.between(now, lockedUntil).toSeconds()
      );
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkForgotPasswordAllowed(String email, HttpServletRequest request) {
    SecurityProperties.ForgotPassword forgotPassword = securityProperties.abuseProtection().forgotPassword();
    String clientFingerprint = resolveClientFingerprint(request);

    checkAndConsume(
        "AUTH_FORGOT_PASSWORD_EMAIL",
        normalizeEmail(email),
        clientFingerprint,
        forgotPassword.emailMaxAttempts(),
        forgotPassword.emailWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
    checkAndConsume(
        "AUTH_FORGOT_PASSWORD_IP",
        GLOBAL_SUBJECT,
        clientFingerprint,
        forgotPassword.ipMaxAttempts(),
        forgotPassword.ipWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkResetPasswordValidateAllowed(String token, HttpServletRequest request) {
    SecurityProperties.ResetPassword resetPassword = securityProperties.abuseProtection().resetPassword();
    checkAndConsume(
        "AUTH_RESET_PASSWORD_VALIDATE",
        token,
        resolveClientFingerprint(request),
        resetPassword.validateMaxAttempts(),
        resetPassword.validateWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkResetPasswordSubmitAllowed(String token, HttpServletRequest request) {
    SecurityProperties.ResetPassword resetPassword = securityProperties.abuseProtection().resetPassword();
    checkAndConsume(
        "AUTH_RESET_PASSWORD_SUBMIT",
        token,
        resolveClientFingerprint(request),
        resetPassword.submitMaxAttempts(),
        resetPassword.submitWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkRefreshAllowed(String rawRefreshToken, HttpServletRequest request) {
    SecurityProperties.Refresh refresh = securityProperties.abuseProtection().refresh();
    checkAndConsume(
        "AUTH_REFRESH_TOKEN",
        rawRefreshToken,
        resolveClientFingerprint(request),
        refresh.tokenMaxAttempts(),
        refresh.tokenWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkMfaStartAllowed(String challengeToken, HttpServletRequest request) {
    SecurityProperties.MfaProtection mfa = securityProperties.abuseProtection().mfa();
    checkAndConsume(
        "AUTH_MFA_START",
        challengeToken,
        resolveClientFingerprint(request),
        mfa.startMaxAttempts(),
        mfa.startWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void checkMfaVerifyAllowed(String challengeToken, HttpServletRequest request) {
    SecurityProperties.MfaProtection mfa = securityProperties.abuseProtection().mfa();
    checkAndConsume(
        "AUTH_MFA_VERIFY",
        challengeToken,
        resolveClientFingerprint(request),
        mfa.verifyMaxAttempts(),
        mfa.verifyWindow(),
        RATE_LIMIT_MESSAGE,
        AUTH_RATE_LIMITED
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RateLimitExceededException.class)
  public void registerFailedLogin(User user) {
    SecurityProperties.Login login = securityProperties.abuseProtection().login();
    Instant now = now();

    if (user.getFirstFailedLoginAt() == null ||
        user.getFirstFailedLoginAt().plus(login.accountWindow()).isBefore(now)) {
      user.setFirstFailedLoginAt(now);
      user.setFailedLoginAttempts(1);
    } else {
      user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
    }

    userRepository.save(user);

    if (user.getFailedLoginAttempts() >= login.accountMaxAttempts()) {
      Instant lockedUntil = now.plus(login.lockDuration());
      user.setLockedUntil(lockedUntil);
      userRepository.save(user);
      throw new RateLimitExceededException(
          ACCOUNT_LOCK_MESSAGE,
          AUTH_ACCOUNT_LOCKED,
          Duration.between(now, lockedUntil).toSeconds()
      );
    }
  }

  public String resolveClientFingerprint(HttpServletRequest request) {
    return clientRequestFingerprintResolver.resolve(request);
  }

  private void checkAndConsume(
      String scope,
      String subject,
      String clientFingerprint,
      int maxAttempts,
      Duration window,
      String message,
      String code
  ) {
    Instant now = now();
    String subjectHash = hash(scope, subject == null || subject.isBlank() ? GLOBAL_SUBJECT : subject);
    String clientHash = hash(scope, clientFingerprint == null || clientFingerprint.isBlank() ? "unknown" : clientFingerprint);

    AuthRateLimitBucket bucket = bucketRepository.findByScopeAndSubjectHashAndClientHash(scope, subjectHash, clientHash)
        .orElseGet(() -> new AuthRateLimitBucket(scope, subjectHash, clientHash, now));

    if (bucket.getBlockedUntil() != null && bucket.getBlockedUntil().isAfter(now)) {
      throw new RateLimitExceededException(
          message,
          code,
          Duration.between(now, bucket.getBlockedUntil()).toSeconds()
      );
    }

    if (bucket.getWindowStartedAt().plus(window).isBefore(now)) {
      bucket.setWindowStartedAt(now);
      bucket.setRequestCount(0);
      bucket.setBlockedUntil(null);
    }

    int nextCount = bucket.getRequestCount() + 1;
    bucket.setRequestCount(nextCount);

    if (nextCount > maxAttempts) {
      Instant blockedUntil = now.plus(window);
      bucket.setBlockedUntil(blockedUntil);
      bucketRepository.save(bucket);
      throw new RateLimitExceededException(
          message,
          code,
          Duration.between(now, blockedUntil).toSeconds()
      );
    }

    bucketRepository.save(bucket);
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
  }

  private String hash(String scope, String rawValue) {
    return tokenHashingService.sha256(scope + ":" + rawValue.trim());
  }

  private Instant now() {
    return clock.instant();
  }
}
