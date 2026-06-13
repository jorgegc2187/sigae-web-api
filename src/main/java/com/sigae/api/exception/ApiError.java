package com.sigae.api.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors,
    String code,
    Long retryAfterSeconds
) {

  public static ApiError of(int status, String error, String message, String path) {
    return new ApiError(Instant.now(), status, error, message, path, Map.of(), null, null);
  }

  public static ApiError of(
      int status,
      String error,
      String message,
      String path,
      String code,
      Long retryAfterSeconds
  ) {
    return new ApiError(Instant.now(), status, error, message, path, Map.of(), code, retryAfterSeconds);
  }

  public static ApiError validation(String message, String path, Map<String, String> validationErrors) {
    return new ApiError(Instant.now(), 400, "Bad Request", message, path, validationErrors, null, null);
  }
}
