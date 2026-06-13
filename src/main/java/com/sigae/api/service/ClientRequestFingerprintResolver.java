package com.sigae.api.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientRequestFingerprintResolver {

  public String resolve(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      String first = forwardedFor.split(",")[0].trim();
      if (!first.isBlank()) {
        return first;
      }
    }

    String remoteAddress = request.getRemoteAddr();
    return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
  }
}
