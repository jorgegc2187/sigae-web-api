package com.sigae.api.exception;

public class RateLimitExceededException extends RuntimeException {

  private final String code;
  private final Long retryAfterSeconds;

  public RateLimitExceededException(String message, String code, Long retryAfterSeconds) {
    super(message);
    this.code = code;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public String getCode() {
    return code;
  }

  public Long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
